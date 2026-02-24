> How the backend is structured and how each layer works. Backend 100% complete as of Day 12.

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
- This is what Spring Security loads to identify who's making a request.

**`Book.java`**
- `copiesAvailable` is the critical field — goes up and down with borrows/returns
- `coverImageUrl` — nullable
- `genre` — nullable String

**`Borrow.java`**
- `@ManyToOne` to User, `@ManyToOne` to Book
- `returnDate` is nullable while book is still borrowed
- `borrowDate`, `dueDate`, and initial `status` are set via `@PrePersist` — the service never touches these

**`Role.java`** — `enum { USER, ADMIN }`

**`BorrowStatus.java`** — `enum { ACTIVE, RETURNED }`

---

## Layer 3 — Repositories (`/repository`)

Interfaces that talk to the database. Spring Data JPA generates queries from method names.

**`UserRepository`**
- `findByEmail(email)` — used during login and JWT filter
- `existsByEmail(email)` — duplicate check during registration

**`BookRepository`**
- Standard `findAll()`, `findById()`, `save()`, `deleteById()`
- `findByIdForUpdate(id)` — custom query with `@Lock(PESSIMISTIC_WRITE)` used during borrow

**`BorrowRepository`**
- `findByUserId(userId)` — user's borrow history
- `findAll()` — admin sees everything
- `countByUserIdAndStatus(userId, ACTIVE)` — enforce max 3 active borrows
- `existsByUserIdAndBookIdAndStatus(userId, bookId, ACTIVE)` — prevent borrowing same book twice

---

## Layer 4 — DTOs (`/dto`)

DTOs are what travel over the wire. Entities are never exposed directly — the `User` entity has a password field you never want sent to a browser.

**Auth DTOs**
- `RegisterDTO` — client sends: `{ firstName, lastName, email, password }`
- `LoginDTO` — client sends: `{ email, password }`
- `AuthResponseDTO` — server returns: `{ token, email, role, firstName }` ✅ firstName included

**Book DTOs**
- `BookCreateDTO` / `BookUpdateDTO` — admin input (includes genre, coverImageUrl)
- `BookResponseDTO` — returned to all callers

**Borrow DTOs**
- `BorrowResponseDTO` — returned to the user for their own borrows. Contains borrow + book fields only.
- `AdminBorrowResponseDTO` — returned to admin for all borrows. Includes `userId`, `userFirstName`, `userLastName`, `userEmail`.

**User DTOs**
- `UserResponseDTO` — returned from admin user endpoints. Contains: `id`, `firstName`, `lastName`, `email`, `role`, `isActive`, `createdAt`. **Never contains password.**

**Why two borrow DTOs?** A regular user calling `GET /borrows/my` doesn't need their own name echoed back. An admin calling `GET /borrows` needs to know who borrowed what. Two DTOs, clean separation.

**Why UserResponseDTO?** The `User` entity contains a BCrypt password hash. Returning the entity directly would expose it. `UserResponseDTO` excludes it entirely.

**`ErrorResponse.java`** — returned on every error: `{ status, message, timestamp }`

---

## Layer 5 — Services (`/service`)

Business logic lives here. Controllers know nothing about rules — that's the service's job.

**`AuthService`**
- `register()` → check email not taken → hash password → save User → return `AuthResponseDTO` with firstName
- `login()` → find user by email → verify BCrypt hash → generate JWT → return `AuthResponseDTO` with firstName

**`BookService`**
- Standard CRUD operations
- `mapToDTO()` private helper — one place to change if `BookResponseDTO` ever changes

**`BorrowService`**

`borrowBook(bookId, userId)`:
1. Lock the book row with `findByIdForUpdate()` — pessimistic lock
2. Check `copiesAvailable > 0` — if not, throw `BusinessException`
3. Check user doesn't have 3 active borrows — if yes, throw `BusinessException`
4. Check user doesn't already borrow this book — if yes, throw `BusinessException`
5. Create `Borrow`, set user + book (everything else handled by `@PrePersist`)
6. Decrement `copiesAvailable`
7. Save both inside `@Transactional`

`returnBook(borrowId, userId)`:
1. Find the borrow record or throw `ResourceNotFoundException`
2. Verify it belongs to this user — if not, throw `BusinessException`
3. Verify status is ACTIVE — if not, throw `BusinessException`
4. Set `returnDate=now`, `status=RETURNED`
5. Increment `copiesAvailable`
6. Save both inside `@Transactional`

`getMyBorrows(userId)` → returns `List<BorrowResponseDTO>`

`getAllBorrows()` → returns `List<AdminBorrowResponseDTO>` with borrower details

**`AdminUserService`**
- `getAllUsers()` → returns `List<UserResponseDTO>`
- `toggleSuspend(userId)` → flips `isActive`. If true → false (suspend). If false → true (unsuspend). Same endpoint, same button in UI.
- `deleteUser(userId)` → hard delete

---

## Layer 6 — Controllers (`/controller`)

Controllers receive HTTP requests, extract parameters, call the service, return responses. Zero business logic.

**`AuthController`** — `/api/v1/auth`
```
POST /register   → public → 201 + AuthResponseDTO { token, email, role, firstName }
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

**`AdminUserController`** — `/api/v1/admin/users` ✅
```
GET    /                  → ADMIN only → 200 + List<UserResponseDTO>
PUT    /{id}/suspend      → ADMIN only → 200 + UserResponseDTO (isActive toggled)
DELETE /{id}              → ADMIN only → 204
```

userId is never passed in the request body — it comes from the JWT via `@AuthenticationPrincipal`.

---

## Layer 7 — Security (`/security` + `/config`)

**`JwtUtils`** — `generateToken(email)`, `extractEmail(token)`, `validateToken(token)`

**`JwtAuthenticationFilter`** — runs on every HTTP request:
1. Read `Authorization: Bearer <token>` header
2. No header → do nothing, continue (public endpoints still work)
3. Token present → validate with `JwtUtils`
4. Valid → load user from DB, set `SecurityContextHolder`
5. Always call `filterChain.doFilter()` — never blocks. Blocking is `SecurityConfig`'s job.

**`CustomUserDetailsService`** — Spring Security contract. `loadUserByUsername(email)` → finds user in DB, returns `UserDetails`.

**`SecurityConfig`** — two layers of authorization:
- Broad rules: auth and GET books are public, everything else requires authentication
- Fine-grained rules via `@PreAuthorize("hasRole('ADMIN')")` on specific controller methods

**401 vs 403:**
- 401 → `authenticationEntryPoint` — token missing or invalid
- 403 → Spring automatic when `@PreAuthorize` fails — valid token, wrong role

**`CorsConfig`** — permits requests from `http://localhost:5173` (React dev server)

**`SwaggerConfig`** — enables JWT Bearer token support in Swagger UI at `/swagger-ui.html`

---

## Layer 8 — Exception Handling (`/exception`)

**`GlobalExceptionHandler`** — `@RestControllerAdvice`. Catches exceptions across all controllers. Returns clean `ErrorResponse` JSON everywhere.

| Exception | HTTP Status | When thrown |
|---|---|---|
| `ResourceNotFoundException` | 404 | Book, user, or borrow not found |
| `BusinessException` | 400 | Rule violation — max borrows, no copies, wrong user |
| `MethodArgumentNotValidException` | 400 | Jakarta validation failed on request body |

---

## Complete File Summary

| File | Location | Status |
|---|---|---|
| `User.java` | `model/` | ✅ has isActive, firstName, lastName |
| `Book.java` | `model/` | ✅ has genre, coverImageUrl |
| `Borrow.java` | `model/` | ✅ @PrePersist sets borrowDate, dueDate, status |
| `Role.java` | `model/` | ✅ USER, ADMIN |
| `BorrowStatus.java` | `model/` | ✅ ACTIVE, RETURNED |
| `UserRepository.java` | `repository/` | ✅ |
| `BookRepository.java` | `repository/` | ✅ with pessimistic lock |
| `BorrowRepository.java` | `repository/` | ✅ |
| `RegisterDTO.java` | `dto/auth/` | ✅ |
| `LoginDTO.java` | `dto/auth/` | ✅ |
| `AuthResponseDTO.java` | `dto/auth/` | ✅ includes firstName |
| `BookCreateDTO.java` | `dto/book/` | ✅ includes genre |
| `BookUpdateDTO.java` | `dto/book/` | ✅ includes genre |
| `BookResponseDTO.java` | `dto/book/` | ✅ includes genre |
| `BorrowResponseDTO.java` | `dto/borrow/` | ✅ user-facing |
| `AdminBorrowResponseDTO.java` | `dto/borrow/` | ✅ admin-facing, includes borrower details |
| `UserResponseDTO.java` | `dto/user/` | ✅ no password field |
| `ErrorResponse.java` | `dto/` | ✅ status, message, timestamp |
| `AuthService.java` | `service/` | ✅ returns firstName in response |
| `BookService.java` | `service/` | ✅ |
| `BorrowService.java` | `service/` | ✅ pessimistic lock, all business rules |
| `AdminUserService.java` | `service/` | ✅ list, toggleSuspend, delete |
| `AuthController.java` | `controller/` | ✅ |
| `BookController.java` | `controller/` | ✅ |
| `BorrowController.java` | `controller/` | ✅ |
| `AdminUserController.java` | `controller/` | ✅ |
| `SecurityConfig.java` | `config/` | ✅ admin/** locked |
| `CorsConfig.java` | `config/` | ✅ permits localhost:5173 |
| `SwaggerConfig.java` | `config/` | ✅ JWT Bearer support |
| `JwtUtils.java` | `security/` | ✅ |
| `JwtAuthenticationFilter.java` | `security/` | ✅ |
| `CustomUserDetailsService.java` | `security/` | ✅ |
| `GlobalExceptionHandler.java` | `exception/` | ✅ |
| `ResourceNotFoundException.java` | `exception/` | ✅ |
| `BusinessException.java` | `exception/` | ✅ |

---

_Last updated: Day 15 ✅ — Backend 100% complete. Do not touch._