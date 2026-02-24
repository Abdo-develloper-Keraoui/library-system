> How the backend is structured and how each layer works. Backend 100% complete as of Day 12. Bug fixes applied Day 17.

---

## The Mental Model

Think of the backend as a restaurant kitchen. The client never enters the kitchen. They order through a waiter **(Controller)**, the waiter passes it to the chef **(Service)**, the chef checks the pantry **(Repository)**, and the pantry talks to the fridge **(Database)**. The security guard at the door **(JwtAuthenticationFilter)** checks your ID before you even reach the waiter.

---

## Layer 1 — Database (PostgreSQL)

Three tables. This is where data lives permanently.

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

- No `authors` table — deliberate decision. Author is a String field on Book.
- `cover_image_url` — nullable String, no file upload
- `genre` — nullable String, not an enum
- `is_active` — boolean on User, defaults to true. Used for suspend/unsuspend.

---

## Layer 2 — Entities (`/model`)

Java classes that map directly to database tables. Hibernate manages the tables automatically.

**`User.java`**
- `role` stored as String via `@Enumerated(EnumType.STRING)`
- `password` is BCrypt hash, never plain text
- `isActive` defaults to `true`. Set to `false` by admin to suspend a user.
- `createdAt` is set automatically via `@PrePersist` — never touched by service code.
- Note: `User` does NOT implement Spring Security's `UserDetails`. That contract is handled entirely by `CustomUserDetailsService`, which builds a `UserDetails` object from the `User` entity on the fly.

**`Book.java`**
- `copiesAvailable` is the critical field — goes up and down with borrows/returns
- `coverImageUrl` — nullable, no `nullable=false` constraint
- `genre` — nullable String, `@Column(length=50)`
- `createdAt` set via `@PrePersist`

**`Borrow.java`**
- `@ManyToOne` to User, `@ManyToOne` to Book
- `returnDate` is nullable while book is still borrowed — no `nullable=false` constraint
- `borrowDate`, `dueDate`, and initial `status` are set via `@PrePersist` using a `BORROW_PERIOD_DAYS = 14` constant — the service never touches these

**`Role.java`** — `enum { USER, ADMIN }`

**`BorrowStatus.java`** — `enum { ACTIVE, RETURNED }`

---

## Layer 3 — Repositories (`/repository`)

Interfaces that talk to the database. Spring Data JPA generates queries from method names.

**`UserRepository`**
- `findByEmail(email)` — used during login and JWT filter
- `existsByEmail(email)` — duplicate check during registration

**`BookRepository`**
- Standard `findAll()`, `findById()`, `save()`, `deleteById()`, `existsById()` — all free from `JpaRepository`
- `findByIdForUpdate(id)` — custom JPQL query with `@Lock(PESSIMISTIC_WRITE)` used during borrow

**`BorrowRepository`**
- `findByUserId(userId)` — user's borrow history
- `findAll()` — admin sees everything
- `countByUserIdAndStatus(userId, ACTIVE)` — enforce max 3 active borrows
- `existsByUserIdAndBookIdAndStatus(userId, bookId, ACTIVE)` — prevent borrowing same book twice
- `deleteByBookId(bookId)` — cascade delete all borrow records for a book before deleting the book. Requires both `@Modifying` and `@Transactional` on the method — without them, Spring throws `No EntityManager with actual transaction available`.
- `deleteByUserId(userId)` — cascade delete all borrow records for a user before deleting the user. Same `@Modifying` + `@Transactional` requirement.

---

## Layer 4 — DTOs (`/dto`)

DTOs are what travel over the wire. Entities are never exposed directly — the `User` entity has a password field you never want sent to a browser.

**Auth DTOs**
- `RegisterDTO` — client sends: `{ firstName, lastName, email, password }`. Password validated: min 8 chars, at least one uppercase letter, at least one digit (`@Pattern`).
- `LoginDTO` — client sends: `{ email, password }`
- `AuthResponseDTO` — server returns: `{ token, email, role, firstName }`. Important: on register, `token` is `null` — a JWT is only generated on login. On login, all four fields are populated.

**Book DTOs**
- `BookCreateDTO` — admin input. `title`, `author`, `isbn` are `@NotBlank`. `pubYear` validated `@Min(-3000)`. `copiesAvailable` validated `@Min(1)`. `genre` and `coverImageUrl` are nullable with no validation.
- `BookUpdateDTO` — PATCH-style: all fields optional (no `@NotBlank`). Only non-null fields are applied by the service.
- `BookResponseDTO` — returned to all callers: `id, title, author, isbn, pubYear, copiesAvailable, coverImageUrl, genre, createdAt`

**Borrow DTOs**
- `BorrowResponseDTO` — returned to the user for their own borrows. Fields: `id, bookTitle, bookAuthor, bookIsbn, bookPubYear, borrowDate, dueDate, returnDate, status`. All book fields are prefixed `book` to avoid naming confusion.
- `AdminBorrowResponseDTO` — returned to admin for all borrows. Same book/borrow fields as above, plus: `userId, userFirstName, userLastName, userEmail`.

**User DTOs**
- `UserResponseDTO` — returned from admin user endpoints. Fields: `id, firstName, lastName, email, role, isActive`. **No `createdAt`. No password field ever.**

**Why two borrow DTOs?** A regular user calling `GET /borrows/my` doesn't need their own name echoed back. An admin calling `GET /borrows` needs to know who borrowed what. Two DTOs, clean separation.

**Why UserResponseDTO?** The `User` entity contains a BCrypt password hash. Returning the entity directly would expose it. `UserResponseDTO` excludes it entirely.

**`ErrorResponse.java`** — returned on every error: `{ status, message, timestamp }`. Timestamp formatted as `yyyy-MM-dd'T'HH:mm:ss` via `@JsonFormat`.

---

## Layer 5 — Services (`/service`)

Business logic lives here. Controllers know nothing about rules — that's the service's job.

**`AuthService`**
- `register()` → check email not taken → hash password → save User with `role=USER` by default → return `AuthResponseDTO` with `token=null` and firstName. Token is null on register — the JWT is only generated during login.
- `login()` → find user by email (throws `BusinessException("Invalid email or password")` if not found) → verify BCrypt hash → generate JWT via `jwtUtils.generateToken(email)` → return `AuthResponseDTO` with token and firstName

**`BookService`**
- Standard CRUD operations
- `updateBook()` is PATCH-style — only non-null fields from `BookUpdateDTO` are applied. Each field is individually guarded with `if (dto.getField() != null)` before updating.
- `deleteBook(id)` — checks `existsById` first (throws `ResourceNotFoundException` if missing), then calls `borrowRepository.deleteByBookId(id)` to cascade delete borrow history, then `bookRepository.deleteById(id)`.
- `validatePubYear()` private helper — throws `BusinessException` if year > current year
- `mapToDTO()` private helper — single place to maintain the `Book → BookResponseDTO` mapping

**`BorrowService`**

`borrowBook(bookId, userId)` — exact execution order from the code:
1. Lock the book row with `bookRepository.findByIdForUpdate(bookId)` — pessimistic write lock
2. Check `copiesAvailable > 0` — if not, throw `BusinessException`
3. Check `countByUserIdAndStatus(userId, ACTIVE) >= 3` — if yes, throw `BusinessException`
4. Check `existsByUserIdAndBookIdAndStatus(userId, bookId, ACTIVE)` — if yes, throw `BusinessException`
5. Fetch the `User` entity: `userRepository.findById(userId)`
6. Check `user.isActive()` — if false (suspended), throw `BusinessException("Your account has been suspended. Please contact an administrator.")`
7. Create `Borrow`, set `borrow.setBook(book)` and `borrow.setUser(user)` — everything else set by `@PrePersist`
8. Decrement `book.setCopiesAvailable(book.getCopiesAvailable() - 1)`
9. `bookRepository.save(book)` then `borrowRepository.save(borrow)` — both inside `@Transactional`

Note: The suspended-user check comes after the business rule checks in the current implementation. A suspended user who also triggers a different rule (no copies, over limit) will see that rule's error rather than the suspension message. This could be refactored to move the check to step 1 in future.

`returnBook(borrowId, userId)`:
1. Find the borrow record or throw `ResourceNotFoundException`
2. Check `borrow.getUser().getId().equals(userId)` — if false, throw `BusinessException`. Note: uses `.equals()` not `==` because `Long` is an object — reference equality would fail.
3. Check `borrow.getStatus().equals(BorrowStatus.ACTIVE)` — if false, throw `BusinessException`
4. Set `borrow.setReturnDate(LocalDate.now())`, `borrow.setStatus(RETURNED)`
5. Increment `copiesAvailable`
6. Save both inside `@Transactional`

`getMyBorrows(userId)` → streams `findByUserId()` → `List<BorrowResponseDTO>`

`getAllBorrows()` → streams `findAll()` → `List<AdminBorrowResponseDTO>` with borrower details

**`AdminUserService`**
- `getAllUsers()` → `userRepository.findAll()` streamed to `List<UserResponseDTO>`
- `suspendUser(userId)` → finds user, flips `isActive` with `user.setActive(!user.isActive())`. Active → suspended. Suspended → unsuspended. Same endpoint, same button in UI. Saves and returns updated `UserResponseDTO`.
- `deleteUser(id)` → checks `existsById` first, calls `borrowRepository.deleteByUserId(id)` to cascade delete borrow history, then `userRepository.deleteById(id)`. `borrowRepository` is constructor-injected alongside `userRepository`.

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

Note: `BorrowController` constructor-injects `UserRepository` directly alongside `BorrowService`. It resolves the logged-in user's identity by calling `userRepository.findByEmail(userDetails.getUsername())` — userId is never passed in the request body, it always comes from the JWT via `@AuthenticationPrincipal UserDetails`.

**`AdminUserController`** — `/api/v1/admin/users`
```
GET    /                  → ADMIN only → 200 + List<UserResponseDTO>
PUT    /{id}/suspend      → ADMIN only → 200 + UserResponseDTO (isActive toggled)
DELETE /{id}              → ADMIN only → 204
```

All three methods have `@PreAuthorize("hasRole('ADMIN')")` — redundant with `SecurityConfig` but makes authorization visible at the point of definition.

---

## Layer 7 — Security (`/security` + `/config`)

**`JwtUtils`** — reads `jwt.secret` and `jwt.expiration` from `application.properties` via `@Value`. Uses JJWT library with HMAC-SHA signing key. Methods: `generateToken(email)`, `extractEmail(token)`, `validateToken(token)`.

**`JwtAuthenticationFilter`** (extends `OncePerRequestFilter`) — runs on every HTTP request:
1. Read `Authorization` header
2. No header, or not starting with `Bearer `, or blank after stripping prefix → `filterChain.doFilter()` and return
3. `jwtUtils.validateToken(token)` — if invalid, skip to step 5
4. Extract email → `customUserDetailsService.loadUserByUsername(email)` → build `UsernamePasswordAuthenticationToken` → set in `SecurityContextHolder`
5. Always call `filterChain.doFilter()` — never blocks. Blocking is `SecurityConfig`'s job.
6. If valid token but user deleted from DB: `UsernameNotFoundException` caught silently, SecurityContext left empty → request gets 401 from SecurityConfig

**`CustomUserDetailsService`** — Spring Security contract. `loadUserByUsername(email)` → finds `User` in DB → builds `UserDetails` using Spring's builder: `.withUsername(email).password(hash).roles(role.name()).build()`. The `User` entity itself does NOT implement `UserDetails`.

**`SecurityConfig`** — two layers of authorization:
- Broad rules: auth endpoints and GET books are public, Swagger UI is public, `/api/v1/admin/**` requires ADMIN role, everything else requires authentication
- Fine-grained rules via `@PreAuthorize("hasRole('ADMIN')")` on specific controller methods

**401 vs 403:**
- 401 → `authenticationEntryPoint` lambda sends `SC_UNAUTHORIZED` — token missing or invalid
- 403 → Spring automatic when `@PreAuthorize` fails — valid token, wrong role

**`CorsConfig`** — permits requests from `http://localhost:5173`. Methods: GET, POST, PUT, DELETE, OPTIONS. `allowCredentials(true)`.

**`SwaggerConfig`** — enables JWT Bearer token support in Swagger UI at `/swagger-ui.html`

---

## Layer 8 — Exception Handling (`/exception`)

**`GlobalExceptionHandler`** — `@RestControllerAdvice`. Catches exceptions across all controllers. Returns clean `ErrorResponse` JSON everywhere.

| Exception | HTTP Status | When thrown |
|---|---|---|
| `ResourceNotFoundException` | 404 | Book, user, or borrow not found |
| `BusinessException` | 400 | Rule violation — max borrows, no copies, wrong user, suspended account |
| `MethodArgumentNotValidException` | 400 | Jakarta validation failed — returns the first field error's message |

---

## Complete File Summary

| File | Location | Status |
|---|---|---|
| `User.java` | `model/` | ✅ isActive, firstName, lastName, @PrePersist createdAt. Does NOT implement UserDetails. |
| `Book.java` | `model/` | ✅ genre, coverImageUrl, @PrePersist createdAt |
| `Borrow.java` | `model/` | ✅ @PrePersist sets borrowDate, dueDate, status via BORROW_PERIOD_DAYS=14 |
| `Role.java` | `model/` | ✅ USER, ADMIN |
| `BorrowStatus.java` | `model/` | ✅ ACTIVE, RETURNED |
| `UserRepository.java` | `repository/` | ✅ |
| `BookRepository.java` | `repository/` | ✅ pessimistic lock via JPQL + @Lock |
| `BorrowRepository.java` | `repository/` | ✅ includes @Modifying deleteByBookId, deleteByUserId |
| `RegisterDTO.java` | `dto/auth/` | ✅ @Pattern validates password strength |
| `LoginDTO.java` | `dto/auth/` | ✅ |
| `AuthResponseDTO.java` | `dto/auth/` | ✅ token is null on register, populated on login |
| `BookCreateDTO.java` | `dto/book/` | ✅ genre, coverImageUrl nullable; copiesAvailable @Min(1) |
| `BookUpdateDTO.java` | `dto/book/` | ✅ all fields optional for PATCH-style update |
| `BookResponseDTO.java` | `dto/book/` | ✅ genre, createdAt included |
| `BorrowResponseDTO.java` | `dto/borrow/` | ✅ user-facing; book fields prefixed "book" |
| `AdminBorrowResponseDTO.java` | `dto/borrow/` | ✅ admin-facing; includes borrower name/email |
| `UserResponseDTO.java` | `dto/user/` | ✅ 6 fields: id, firstName, lastName, email, role, isActive. No createdAt, no password. |
| `ErrorResponse.java` | `dto/` | ✅ status, message, timestamp (@JsonFormat) |
| `AuthService.java` | `service/` | ✅ token null on register, JWT generated on login |
| `BookService.java` | `service/` | ✅ PATCH-style update, cascade delete |
| `BorrowService.java` | `service/` | ✅ pessimistic lock; suspension check after business rules (steps 1–4) |
| `AdminUserService.java` | `service/` | ✅ suspendUser() toggles isActive; deleteUser() cascades |
| `AuthController.java` | `controller/` | ✅ |
| `BookController.java` | `controller/` | ✅ |
| `BorrowController.java` | `controller/` | ✅ injects UserRepository to resolve identity from JWT |
| `AdminUserController.java` | `controller/` | ✅ calls suspendUser() |
| `SecurityConfig.java` | `config/` | ✅ admin/** locked at route level + @PreAuthorize on methods |
| `CorsConfig.java` | `config/` | ✅ permits localhost:5173 |
| `SwaggerConfig.java` | `config/` | ✅ JWT Bearer support |
| `JwtUtils.java` | `security/` | ✅ reads from application.properties via @Value |
| `JwtAuthenticationFilter.java` | `security/` | ✅ handles missing header, blank token, deleted-user edge cases |
| `CustomUserDetailsService.java` | `security/` | ✅ builds UserDetails from User entity |
| `GlobalExceptionHandler.java` | `exception/` | ✅ |
| `ResourceNotFoundException.java` | `exception/` | ✅ |
| `BusinessException.java` | `exception/` | ✅ |

---

_Last updated: Day 17 ✅ — Backend 100% complete. Do not touch._