# Backend Guide

How the backend is structured and how each layer works.

---

## Mental Model

The backend is a standard layered Spring Boot application. The HTTP request enters through a security filter, is routed by a controller, processed by a service, persisted via a repository, and reaches PostgreSQL.

```
Request → JwtAuthFilter → Controller → Service → Repository → PostgreSQL
```

The `JwtAuthenticationFilter` runs on every request before it reaches any controller. It reads the `Authorization` header, validates the token, and sets the authentication context. If there is no token, the request proceeds unauthenticated — it is `SecurityConfig`'s job to block it if authentication is required.

---

## Layer 1 — Database (PostgreSQL)

Three tables:

```
users                   books                   borrows
─────                   ─────                   ───────
id                      id                      id
first_name              title                   user_id  → FK
last_name               author (String)         book_id  → FK
email (unique)          isbn                    borrow_date
password (BCrypt)       pub_year                due_date
role (USER/ADMIN)       copies_available        return_date (nullable)
is_active (boolean)     cover_image_url (null)  status (ACTIVE/RETURNED)
created_at              genre (null)
                        created_at
```

- No `authors` table — author is a `String` field on `Book`
- `cover_image_url` — nullable, no file upload
- `genre` — nullable `String`, not an enum
- `is_active` — boolean on `User`, defaults to `true`, used for suspend/unsuspend

---

## Layer 2 — Entities (`/model`)

Java classes mapped to database tables. Hibernate manages schema via `spring.jpa.hibernate.ddl-auto`.

**`User.java`**
- `role` stored as String via `@Enumerated(EnumType.STRING)`
- `password` is a BCrypt hash, never plain text
- `isActive` defaults to `true`; set to `false` by admin to suspend
- `createdAt` set automatically via `@PrePersist`
- `User` does NOT implement `UserDetails` — that contract is handled by `CustomUserDetailsService`

**`Book.java`**
- `copiesAvailable` is the critical field — goes up and down with borrows/returns
- `coverImageUrl` — nullable
- `genre` — nullable, `@Column(length=50)`
- `createdAt` set via `@PrePersist`

**`Borrow.java`**
- `@ManyToOne` to `User`, `@ManyToOne` to `Book`
- `returnDate` is nullable while book is still borrowed
- `borrowDate`, `dueDate`, and initial `status` are all set via `@PrePersist` using `BORROW_PERIOD_DAYS = 14` — the service never sets these directly

**`Role.java`** — `enum { USER, ADMIN }`

**`BorrowStatus.java`** — `enum { ACTIVE, RETURNED }`

---

## Layer 3 — Repositories (`/repository`)

Interfaces extending `JpaRepository`. Spring Data JPA generates queries from method names.

**`UserRepository`**
- `findByEmail(email)` — used during login and in the JWT filter
- `existsByEmail(email)` — duplicate check during registration

**`BookRepository`**
- Standard `findAll()`, `findById()`, `save()`, `deleteById()`, `existsById()` from `JpaRepository`
- `findByIdForUpdate(id)` — custom JPQL with `@Lock(PESSIMISTIC_WRITE)` for borrow concurrency

**`BorrowRepository`**
- `findByUserId(userId)` — user's borrow history
- `findAll()` — admin view of all borrows
- `countByUserIdAndStatus(userId, ACTIVE)` — enforce max 3 active borrows
- `existsByUserIdAndBookIdAndStatus(userId, bookId, ACTIVE)` — prevent duplicate borrows
- `deleteByBookId(bookId)` — cascade delete before deleting a book. Requires `@Modifying` + `@Transactional`
- `deleteByUserId(userId)` — cascade delete before deleting a user. Same requirements

---

## Layer 4 — DTOs (`/dto`)

DTOs are what travel over the wire. Entities are never exposed directly — the `User` entity contains a hashed password that should never reach a client.

**Auth DTOs**
- `RegisterDTO` — `{ firstName, lastName, email, password }`. Password: `@Pattern` requires min 8 chars, at least one uppercase, at least one digit
- `LoginDTO` — `{ email, password }`
- `AuthResponseDTO` — `{ token, email, role, firstName }`. `token` is `null` on register — a JWT is only generated on login

**Book DTOs**
- `BookCreateDTO` — `title`, `author`, `isbn` are `@NotBlank`. `pubYear` has `@Min(-3000)`. `copiesAvailable` has `@Min(1)`. `genre` and `coverImageUrl` are nullable
- `BookUpdateDTO` — all fields optional (PATCH-style). Only non-null fields are applied by the service
- `BookResponseDTO` — `id, title, author, isbn, pubYear, copiesAvailable, coverImageUrl, genre, createdAt`

**Borrow DTOs**
- `BorrowResponseDTO` — user-facing. Fields: `id, bookTitle, bookAuthor, bookIsbn, bookPubYear, borrowDate, dueDate, returnDate, status`. Book fields are prefixed `book`
- `AdminBorrowResponseDTO` — admin-facing. Same fields as above plus: `userId, userFirstName, userLastName, userEmail`

**User DTOs**
- `UserResponseDTO` — `id, firstName, lastName, email, role, isActive`. No password, no `createdAt`

**`ErrorResponse.java`** — returned on all errors: `{ status, message, timestamp }`. Timestamp formatted via `@JsonFormat`.

---

## Layer 5 — Services (`/service`)

Business logic lives here. Controllers know nothing about rules.

**`AuthService`**
- `register()` → check email not taken → hash password → save `User` with `role=USER` → return `AuthResponseDTO` with `token=null`
- `login()` → find user by email (throws `BusinessException("Invalid email or password")` if not found) → verify BCrypt hash → generate JWT → return `AuthResponseDTO` with all four fields

**`BookService`**
- Standard CRUD
- `updateBook()` is PATCH-style — each field guarded individually with `if (dto.getField() != null)` before updating
- `deleteBook(id)` — checks `existsById` first, calls `borrowRepository.deleteByBookId(id)`, then `bookRepository.deleteById(id)`
- `validatePubYear()` — throws `BusinessException` if year > current year
- `mapToDTO()` — single place for the `Book → BookResponseDTO` mapping

**`BorrowService`**

`borrowBook(bookId, userId)` — exact execution order:
1. Lock the book row — `bookRepository.findByIdForUpdate(bookId)` (pessimistic write lock)
2. Check `copiesAvailable > 0`
3. Check `countByUserIdAndStatus(userId, ACTIVE) >= 3`
4. Check `existsByUserIdAndBookIdAndStatus(userId, bookId, ACTIVE)`
5. Fetch the `User` entity, check `user.isActive()` — throws `BusinessException` if suspended
6. Create `Borrow` entity — `@PrePersist` sets `borrowDate`, `dueDate`, `status`
7. Decrement `copiesAvailable`, save both inside `@Transactional`

`returnBook(borrowId, userId)`:
1. Find borrow by ID
2. Check `borrow.getUser().getId().equals(userId)` — ownership check
3. Check `borrow.getStatus() != ACTIVE` — cannot return an already-returned book
4. Set `returnDate = LocalDate.now()`, `status = RETURNED`
5. Increment `copiesAvailable`
6. Save both inside `@Transactional`

**`AdminUserService`**
- `getAllUsers()` → `findAll()` streamed to `List<UserResponseDTO>`
- `suspendUser(userId)` → flips `isActive` with `user.setActive(!user.isActive())`. Active → suspended. Suspended → active. Same endpoint, same button
- `deleteUser(id)` → checks `existsById`, calls `borrowRepository.deleteByUserId(id)`, then `userRepository.deleteById(id)`

---

## Layer 6 — Controllers (`/controller`)

Controllers receive HTTP requests, extract parameters, call the service, return responses. Zero business logic.

**`AuthController`** — `/api/v1/auth`
```
POST /register   → public → 201 + AuthResponseDTO { token: null, email, role, firstName }
POST /login      → public → 200 + AuthResponseDTO { token, email, role, firstName }
```

**`BookController`** — `/api/v1/books`
```
GET    /          → public       → 200 + List<BookResponseDTO>
GET    /{id}      → public       → 200 + BookResponseDTO
POST   /          → ADMIN only   → 201 + BookResponseDTO
PUT    /{id}      → ADMIN only   → 200 + BookResponseDTO
DELETE /{id}      → ADMIN only   → 204
```

**`BorrowController`** — `/api/v1/borrows`
```
POST  /{bookId}/borrow    → authenticated   → 200 + BorrowResponseDTO
PUT   /{borrowId}/return  → authenticated   → 200 + BorrowResponseDTO
GET   /my                 → authenticated   → 200 + List<BorrowResponseDTO>
GET   /                   → ADMIN only      → 200 + List<AdminBorrowResponseDTO>
```

Note: `BorrowController` resolves the logged-in user's identity from the JWT via `@AuthenticationPrincipal UserDetails` — userId is never passed in the request body.

**`AdminUserController`** — `/api/v1/admin/users`
```
GET    /                  → ADMIN only → 200 + List<UserResponseDTO>
PUT    /{id}/suspend      → ADMIN only → 200 + UserResponseDTO (isActive toggled)
DELETE /{id}              → ADMIN only → 204
```

All three methods carry `@PreAuthorize("hasRole('ADMIN')")` — redundant with `SecurityConfig` but makes authorization visible at the point of definition.

---

## Layer 7 — Security (`/security` + `/config`)

**`JwtUtils`** — reads `jwt.secret` and `jwt.expiration` from `application.properties` via `@Value`. Uses JJWT with HMAC-SHA. Methods: `generateToken(email)`, `extractEmail(token)`, `validateToken(token)`.

**`JwtAuthenticationFilter`** (extends `OncePerRequestFilter`) — runs on every HTTP request:
1. Read `Authorization` header
2. No header, or not starting with `Bearer `, or blank after stripping prefix → `filterChain.doFilter()` and return
3. `jwtUtils.validateToken(token)` — if invalid, skip to step 5
4. Extract email → `customUserDetailsService.loadUserByUsername(email)` → build `UsernamePasswordAuthenticationToken` → set in `SecurityContextHolder`
5. Always call `filterChain.doFilter()` — never blocks directly. Blocking is `SecurityConfig`'s responsibility
6. If valid token but user deleted from DB: `UsernameNotFoundException` caught silently, `SecurityContext` left empty → 401 from `SecurityConfig`

**`CustomUserDetailsService`** — `loadUserByUsername(email)` → finds `User` in DB → builds `UserDetails` using Spring's builder: `.withUsername(email).password(hash).roles(role.name()).build()`

**`SecurityConfig`**
- URL-level rules: auth endpoints and GET books are public; Swagger is public; `/api/v1/admin/**` requires `ADMIN`; everything else requires authentication
- Fine-grained rules: `@PreAuthorize` on specific controller methods

**401 vs 403:**
- 401 → `authenticationEntryPoint` lambda sends `SC_UNAUTHORIZED` — token missing or invalid
- 403 → Spring automatic when `@PreAuthorize` fails — valid token, wrong role

**`CorsConfig`** — permits requests from `http://localhost:5173`. Methods: GET, POST, PUT, DELETE, OPTIONS. `allowCredentials(true)`.

**`SwaggerConfig`** — enables JWT Bearer token support in the Swagger UI at `/swagger-ui.html`.

---

## Layer 8 — Exception Handling (`/exception`)

`GlobalExceptionHandler` (`@RestControllerAdvice`) catches exceptions across all controllers and returns clean `ErrorResponse` JSON.

| Exception | HTTP Status | When thrown |
|---|---|---|
| `ResourceNotFoundException` | 404 | Book, user, or borrow not found |
| `BusinessException` | 400 | Rule violation — max borrows, no copies, wrong user, suspended account |
| `MethodArgumentNotValidException` | 400 | Jakarta validation failed — returns the first field error's message |

---

## DataSeeder

`DataSeeder.java` seeds the database on first startup only (`if (repository.count() > 0) return`). It creates two users and 33 books with cover image URLs. To reseed, run `docker compose down -v` to wipe the volume first.

Seeded accounts:
- Admin: `Ahmed@google.com` / `Password123`
- User: `Ahmed@User.com` / `Password123`
