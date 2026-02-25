# Architectural Decisions — Backend

A log of technical decisions made during the backend build. Each entry documents what was decided, why, and what was ruled out.

---

## Scope Decisions

### 1. Author is a String field on Book — not a separate entity

**Decision:** Author is stored as a plain `String` on the `Book` entity. No `Author` table, no `Author` CRUD, no relationships.

**Why:** A dedicated `Author` entity would add significant development time while demonstrating no new architectural concepts beyond what Book CRUD already covers. Author-as-string is a deliberate scope boundary, not an oversight.

---

### 2. Borrow statuses: ACTIVE and RETURNED only — no OVERDUE

**Decision:** `BorrowStatus` enum has two values: `ACTIVE` and `RETURNED`.

**Why:** `OVERDUE` requires either a scheduled background job or on-read calculation logic — both add complexity disproportionate to the value gained. Two statuses are sufficient to demonstrate state management in a borrowing flow. Can be added as a future enhancement.

---

### 3. Book cover image stored as a nullable URL string — no file upload

**Decision:** `Book` has a nullable `coverImageUrl` String field. No multipart upload, no file storage.

**Why:** A cover image URL makes the UI significantly better. Actual file upload (multipart requests, S3/disk storage, serving files) introduces infrastructure complexity that is out of scope. Nullable because not every book entry needs an image.

---

### 4. `copiesAvailable` as an int — no separate boolean availability flag

**Decision:** Availability is derived directly from `copiesAvailable > 0`. No `isAvailable` boolean.

**Why:** Two fields representing the same thing will eventually get out of sync. A boolean is redundant — it can be derived from the integer. Redundant state is a liability.

---

### 5. Genre is a nullable String — not an enum

**Decision:** `Book` has a nullable `genre` String field. No enum, no separate table.

**Why:** An enum is too rigid for a library catalogue — adding new genres would require a code change and redeployment. A string lets admins enter values freely. Nullable because not every book needs a genre assigned immediately.

---

### 6. Admin user management: suspend/unsuspend toggle and delete

**Decision:** Three admin endpoints — list all users, toggle suspend/unsuspend (`isActive` toggle), delete a user. `User` entity has an `isActive` boolean defaulting to `true`.

**Why:** Suspend is more realistic than hard-delete for most violations — an admin can revoke borrowing access without destroying the user's borrow history. The same endpoint toggles both directions (active → suspend, suspended → unsuspend), enabling a clean single-button UI. Delete handles full cleanup when needed.

**Endpoints:**
```
GET    /api/v1/admin/users
PUT    /api/v1/admin/users/{id}/suspend
DELETE /api/v1/admin/users/{id}
```

---

### 7. firstName included in AuthResponseDTO

**Decision:** Login and register responses both return `{ token, email, role, firstName }`.

**Why:** The Navbar needs to display the user's first name on every page. Without it in the auth response, the frontend would need a separate `GET /users/me` call after every login. Returning it in the auth response makes it immediately available — no extra round trip.

---

## Technology Decisions

### 8. Spring Security + JWT authentication

**Decision:** Authentication uses Spring Security with JWT tokens.

**Why:** In the Java ecosystem, Spring Security + JWT is the industry standard for stateless REST API authentication.

---

### 9. Only PostgreSQL runs in Docker during development

**Decision:** PostgreSQL runs in Docker. The Spring Boot application and frontend run natively during development.

**Why:** Fast feedback loops. Only the database is infrastructure — it belongs in a container. Everything is containerized for deployment. This mirrors how real development teams work.

---

### 10. Full Docker Compose setup as a deployment-phase concern

**Decision:** Dockerfiles and `docker-compose.yml` are written once the application is feature-complete.

**Why:** Writing Docker configs during active development adds unnecessary build cycles. It is a deployment concern, not a development concern.

---

### 11. CI/CD with GitHub Actions — minimal pipeline

**Decision:** A basic GitHub Actions pipeline runs build + tests on every push to `main`.

**Why:** Demonstrates awareness of modern CI/CD workflows without over-engineering. A simple pipeline that passes is more valuable than a complex one that never gets finished.

---

## Architecture Decisions

### 12. Layered architecture: Controller → Service → Repository

**Decision:** Standard three-layer Spring Boot architecture.

**Why:** Controllers handle HTTP. Services handle business logic. Repositories handle data access. Clean separation of concerns makes the code testable, maintainable, and straightforward to reason about.

---

### 13. Service methods throw exceptions on "not found" — never return Optional

**Decision:** Services throw `ResourceNotFoundException` instead of returning `Optional`.

**Why:** In a REST API, "not found" is an error — the correct response is 404, not 200 with an empty body. Returning `Optional` pushes the decision to the controller. Throwing an exception lets `GlobalExceptionHandler` catch it once and return a consistent 404 everywhere.

---

### 14. Custom exception classes — not generic RuntimeException

**Decision:** `ResourceNotFoundException` and `BusinessException` instead of raw `RuntimeException`.

**Why:** A generic `RuntimeException` gives Spring no information about what went wrong — it defaults to 500. Custom exceptions carry meaning and let `GlobalExceptionHandler` map each one to the correct HTTP status (404, 400) in one place.

---

### 15. Password hashing in the service layer — not controller or entity

**Decision:** BCrypt hashing happens in `AuthService`.

**Why:** The service layer is where business logic lives. Hashing in the controller would require every code path that creates a user to remember to hash. In the service, it happens once, unconditionally, in one place.

---

### 16. Validation errors handled in GlobalExceptionHandler — not per-controller

**Decision:** `MethodArgumentNotValidException` is caught once in `GlobalExceptionHandler`.

**Why:** Without a central handler, Spring returns its own inconsistent error format. Catching it once means every endpoint returns the same `ErrorResponse` shape on validation failure.

---

### 17. Role-based authorization: SecurityConfig for broad rules + @PreAuthorize for fine-grained rules

**Decision:** `SecurityConfig` handles URL-pattern authorization (`/api/v1/admin/**` requires `ADMIN`). `@PreAuthorize("hasRole('ADMIN')")` is also applied directly on controller methods.

**Why:** `@PreAuthorize` puts the authorization rule at the point of definition, right next to the method it protects. A developer reading `BookController.java` can immediately see who can call each method, without opening a separate config file. Redundancy is intentional — defense in depth.

---

### 18. Stateless API with JWT — no server-side sessions

**Decision:** No sessions are created or stored on the server.

**Why:** Session-based auth makes the server the source of truth — if the server goes down, sessions are lost. With JWT, the token itself is the session. Any server can validate it independently using the shared secret key. No shared state, no sticky sessions, scales horizontally.

---

### 19. Two borrow DTOs — user-facing vs admin-facing

**Decision:** `BorrowResponseDTO` for regular users, `AdminBorrowResponseDTO` for admins.

**Why:** A regular user calling `GET /borrows/my` does not need their own name echoed back. An admin calling `GET /borrows` needs to know who borrowed what — `userId`, `userFirstName`, `userLastName`, `userEmail`. Two DTOs, clean separation, no leakage risk.

---

### 20. UserResponseDTO — separate DTO for user data

**Decision:** A dedicated `UserResponseDTO` is returned from admin user endpoints. Never the `User` entity directly.

**Why:** The `User` entity contains a BCrypt password hash. Returning the entity directly would expose it in every API response. `UserResponseDTO` includes only: `id`, `firstName`, `lastName`, `email`, `role`, `isActive`. Password is never sent over the wire.

---

### 21. Pessimistic locking on borrow operations

**Decision:** `BookRepository.findByIdForUpdate()` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

**Why:** Two users trying to borrow the last copy simultaneously is a real race condition. Without locking, both threads read `copiesAvailable = 1`, both pass the availability check, and both create a borrow — leaving `copiesAvailable = -1`. Pessimistic locking issues `SELECT ... FOR UPDATE` at the database level. The second transaction blocks until the first commits, then reads the updated value of 0 and fails the check cleanly.

---

### 22. @PrePersist for automatic borrow field initialization

**Decision:** `borrowDate`, `dueDate`, and initial `status` are set in `@PrePersist` on `Borrow.java`, not in `BorrowService`.

**Why:** `@PrePersist` is a JPA lifecycle hook — it fires automatically just before the entity is saved. The service cannot forget to set these fields. Guarantees consistency regardless of how a `Borrow` is created.

---

### 23. Admin created by direct DB update — not a special endpoint

**Decision:** To create an admin, register normally then run `UPDATE users SET role = 'ADMIN' WHERE email = '...'` in PostgreSQL. In development, `DataSeeder.java` handles this automatically.

**Why:** A "register as admin" endpoint would be a security vulnerability. There is no legitimate reason for a self-service admin creation flow.

---

### 24. Ownership check in returnBook uses ID comparison — not a DB lookup

**Decision:** `returnBook()` verifies the borrow belongs to the caller with `borrow.getUser().getId().equals(userId)`.

**Why:** The `borrow` entity is already loaded — it contains the full `User` relationship. Calling `userRepository.findById(userId)` just to compare IDs would be a wasted database round-trip. Comparing IDs from what is already loaded is simpler and faster. Note: `Long` is an object — `.equals()` must be used, not `==`.

---

### 25. Swagger UI for interactive API documentation

**Decision:** Added `springdoc-openapi-starter-webmvc-ui` and `SwaggerConfig.java` with JWT Bearer token support.

**Why:** Without documentation, anyone evaluating the API must read source code. Swagger generates an interactive UI at `/swagger-ui.html` where endpoints can be browsed and tested directly in the browser using a real JWT token.

---

## Key Concepts

### What is a JWT?

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSJ9.abc123xyz
        HEADER                        PAYLOAD                  SIGNATURE
```

| Part | Contents |
|---|---|
| **Header** | Algorithm used to sign (e.g. HS256) |
| **Payload** | Data — email, role, expiry. Base64 encoded, not encrypted |
| **Signature** | Cryptographic hash of header + payload, signed with the server's secret key |

If someone tampers with the payload, the signature breaks. The server detects it instantly.

---

### Authentication vs Authorization

| Concept | Question | Where it lives |
|---|---|---|
| **Authentication** | Who are you? | `JwtAuthenticationFilter` |
| **Authorization** | What are you allowed to do? | `SecurityConfig` + `@PreAuthorize` |

---

### Pessimistic Locking — The Race Condition

```
Thread A reads copiesAvailable = 1  ✅ passes check
Thread B reads copiesAvailable = 1  ✅ passes check  (no locking)
Thread A writes copiesAvailable = 0
Thread B writes copiesAvailable = -1  ❌ data corruption
```

With `SELECT ... FOR UPDATE`:
```
Thread A locks row
Thread B blocks...
Thread A commits (copiesAvailable = 0)
Thread B reads 0 → fails check cleanly  ✅
```

---

### Why DTOs, Not Entities

Entities are the internal database representation. DTOs are the API contract. Returning entities directly exposes your entire schema — including hashed passwords, internal IDs, and fields that should never be client-visible. DTOs give full control over what is sent over the wire.
