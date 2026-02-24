> Every major decision made during this project — **what** was decided and **why**. Also includes key concepts needed to understand the backend internals. Useful for interview prep, future reference, and proving you actually thought about your choices.

---

## 📦 Scope Decisions

### 1. Author is a String field on Book — not a separate entity

**Decision:** Author is stored as a plain `String` on the `Book` entity. No `Author` table, no `Author` CRUD, no relationships.

**Why:** A dedicated `Author` entity would cost 4–6 hours and demonstrate zero new architectural concepts beyond what Book CRUD already covers. In a 70-hour project, that time is better spent on authentication, borrowing logic, and deployment.

---

### 2. Borrow statuses: ACTIVE and RETURNED only — no OVERDUE

**Decision:** `BorrowStatus` enum has two values: `ACTIVE` and `RETURNED`.

**Why:** `OVERDUE` requires either a scheduled background job or on-read calculation logic — both add complexity disproportionate to the portfolio value gained. Two statuses are enough to demonstrate state management in a borrowing flow. Can be added later as an enhancement.

---

### 3. Book cover image stored as a nullable URL string — no file upload

**Decision:** `Book` has a nullable `coverImageUrl` String field. No multipart upload, no file storage.

**Why:** A cover image makes the demo look significantly better. Storing it as a URL costs 5 minutes. Actual file upload (multipart requests, S3/disk storage, serving files) would cost 4–6 hours and introduces infrastructure complexity irrelevant to the core purpose. Nullable because not every book entry needs an image.

---

### 4. `copiesAvailable` as an int — no separate boolean availability flag

**Decision:** Availability is derived directly from `copiesAvailable > 0`. No `isAvailable` boolean.

**Why:** Two fields representing the same thing will eventually get out of sync and cause bugs. A boolean is redundant — you can derive it from the int. Redundant state is always a liability.

---

### 5. Genre is a nullable String — not an enum

**Decision:** `Book` has a nullable `genre` String field. No enum, no separate table.

**Why:** An enum is too rigid for a library catalogue — adding "Graphic Novel" or "Travel" later would require a code change and redeployment. A String lets the admin type freely. For a portfolio project where one admin controls all data entry, consistency is maintained by discipline, not enforcement. Nullable because not every book needs a genre assigned immediately.

---

### 6. Admin user management: suspend/unsuspend toggle and delete ✅ COMPLETE

**Decision:** Three admin endpoints — list all users, toggle suspend/unsuspend (`isActive` toggle), delete a user. `User` entity has an `isActive` boolean field defaulting to `true`.

**Why:** Demonstrates admin power beyond book management. Suspend is more realistic than hard-delete for most violations — an admin can revoke borrowing access without losing the user's history. The same endpoint toggles both directions (if active → suspend, if suspended → unsuspend), which enables a clean single button in the UI. Delete is also available for full cleanup. Both are ADMIN-only and locked down in `SecurityConfig`.

**Endpoints:**
```
GET    /api/v1/admin/users              ← returns List<UserResponseDTO>
PUT    /api/v1/admin/users/{id}/suspend ← toggles isActive
DELETE /api/v1/admin/users/{id}         ← hard delete
```

---

### 7. firstName added to AuthResponseDTO ✅

**Decision:** Login and register responses both return `{ token, email, role, firstName }`.

**Why:** The Navbar needs to display the user's first name on every page. If `firstName` is not in the auth response, the frontend would need a separate `GET /api/v1/users/me` call after every login just to show a name. Returning it in the auth response means it's available immediately — no extra round trip. The frontend stores it in `localStorage` alongside the token.

---

## 🛠️ Technology Decisions

### 8. React + Vite for frontend — not Next.js

**Decision:** Frontend is built with plain React + Vite.

**Why:** Zero JavaScript experience going in. Next.js adds SSR, file-based routing, and its own abstractions — three layers of new concepts at once. React + Vite means only learning components, state, and API calls. The frontend is ~20% of this project; the backend is where the interview value lives.

---

### 9. Spring Security + JWT authentication

**Decision:** Authentication uses Spring Security with JWT tokens.

**Why:** In the Java ecosystem, Spring Security + JWT is the industry standard. This is what recruiters and interviewers expect to see on a Java backend project.

---

### 10. Only PostgreSQL runs in Docker during development

**Decision:** PostgreSQL runs in Docker. Spring Boot runs from IntelliJ. React runs via `npm run dev`.

**Why:** Fast feedback loops during development. Only the database is infrastructure — it belongs in a container. Everything gets containerized at deployment. This mirrors how real development teams work.

---

### 11. Full Docker Compose setup is a deployment-phase task

**Decision:** Dockerfiles and `docker-compose.yml` are written once the application is feature-complete.

**Why:** Writing Docker configs during active development adds unnecessary build cycles. It's a deployment concern, not a dev concern. Doing it at the end proves we understand containerization without slowing down daily progress.

---

### 12. CI/CD with GitHub Actions — minimal pipeline

**Decision:** A basic GitHub Actions pipeline runs build + tests on every push to `main`.

**Why:** Takes ~1–2 hours, sends a strong signal to recruiters that we understand modern development workflows. Kept simple — no multi-stage deployments or complex configurations.

---

## 🏗️ Architecture Decisions

### 13. Layered architecture: Controller → Service → Repository

**Decision:** Standard three-layer Spring Boot architecture.

**Why:** Controllers handle HTTP. Services handle business logic. Repositories handle data access. Clean separation of concerns makes the code testable, maintainable, and easy to explain. This is what interviewers expect from a Java backend project.

---

### 14. Service methods throw exceptions on "not found" — never return Optional

**Decision:** Services throw `ResourceNotFoundException` instead of returning `Optional`.

**Why:** In a REST API, "not found" is an error — the correct response is 404, not 200 with an empty body. Returning `Optional` pushes the decision to the controller, so every controller must handle it separately. Throwing an exception lets `GlobalExceptionHandler` catch it once and return a consistent 404 everywhere.

---

### 15. Custom exception classes — not generic RuntimeException

**Decision:** `ResourceNotFoundException`, `BusinessException` instead of raw `RuntimeException`.

**Why:** A generic `RuntimeException` gives Spring no information about what went wrong — it defaults to 500 regardless. Custom exceptions carry meaning and let `GlobalExceptionHandler` map each one to the correct HTTP status (404, 400). One place to change, consistent behaviour everywhere.

---

### 16. Password hashing in the service layer — not controller or entity

**Decision:** BCrypt hashing happens in `AuthService`, not `AuthController` or `User`.

**Why:** The service layer is where business logic lives. If hashing were done in the controller, every controller that creates a user would have to remember to hash — that's duplication and a security risk. In the service, it happens once, every time, in one place.

---

### 17. Validation errors handled in GlobalExceptionHandler — not per-controller

**Decision:** `MethodArgumentNotValidException` is caught once in `GlobalExceptionHandler`.

**Why:** Without a central handler, Spring returns its own inconsistent error format. Catching it once means every endpoint returns the same `ErrorResponse` shape on validation failure. One place to change, consistent behaviour everywhere.

---

### 18. Role-based authorization: SecurityConfig for broad rules + @PreAuthorize for fine-grained rules

**Decision:** Authorization uses both `SecurityConfig` and `@PreAuthorize` together.

`SecurityConfig` handles broad structural rules — auth endpoints are public, GET book endpoints are public, everything else requires authentication.

`@PreAuthorize("hasRole('ADMIN')")` handles fine-grained rules directly on controller methods — POST/PUT/DELETE books, GET all borrows, all admin user management endpoints.

**Why:** Putting all rules in `SecurityConfig` creates a hidden coupling problem — a developer reading `BookController.java` has no idea who can call each method without opening a separate file. `@PreAuthorize` puts the authorization rule at the point of definition, right next to the method it protects. This is the industry-standard approach in Spring Boot projects.

---

### 19. Stateless API with JWT — no server-side sessions

**Decision:** The API uses stateless JWT authentication. No sessions are created or stored on the server.

**Why:** Session-based auth makes the server the source of truth — sticky sessions fail if that server goes down. JWT solves this cleanly. The token itself is the session — generated at login, stored client-side, sent with every request. Any server can validate it independently using the shared secret key. No shared state, scales to any number of servers.

---

### 20. Two borrow DTOs — user-facing vs admin-facing

**Decision:** `BorrowResponseDTO` for regular users, `AdminBorrowResponseDTO` for admins.

**Why:** A regular user calling `GET /borrows/my` doesn't need their own name echoed back — that's noise. An admin calling `GET /borrows` needs to know who borrowed what — `userId`, `userFirstName`, `userLastName`, `userEmail`. Two DTOs, two mappers, clean separation. The endpoints are protected differently so there's no leakage risk.

---

### 21. UserResponseDTO — separate DTO for user data

**Decision:** A dedicated `UserResponseDTO` is returned from admin user endpoints. Never the `User` entity directly.

**Why:** The `User` entity contains a BCrypt password hash. Returning the entity directly would expose it in every admin API response. `UserResponseDTO` includes only: `id`, `firstName`, `lastName`, `email`, `role`, `isActive`, `createdAt`. Password is never sent over the wire.

---

### 22. Pessimistic locking on borrow operations

**Decision:** `BookRepository.findByIdForUpdate()` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

**Why:** Two users trying to borrow the last copy simultaneously is a real race condition. Without locking, both threads read `copiesAvailable = 1`, both pass the check, and both create a borrow — leaving `copiesAvailable = -1`. Pessimistic locking issues `SELECT ... FOR UPDATE` at the database level. The second thread blocks until the first transaction commits, then reads the updated value of 0 and fails the check cleanly. Data integrity preserved.

---

### 23. @PrePersist for automatic borrow field initialization

**Decision:** `borrowDate`, `dueDate`, and initial `status` are set in `@PrePersist` on `Borrow.java`, not in `BorrowService`.

**Why:** If the service sets these fields, every code path that creates a `Borrow` must remember to set them. `@PrePersist` is a JPA lifecycle hook — it fires automatically just before the entity is saved. The service can't forget it. Guarantees consistency regardless of how a `Borrow` is created.

---

### 24. Admin created by direct DB update — not a special endpoint

**Decision:** To create an admin, register normally then run `UPDATE users SET role = 'ADMIN' WHERE email = '...'` in PostgreSQL.

**Why:** A "register as admin" endpoint would be a massive security vulnerability. There's no legitimate reason for a self-service admin creation flow. For a portfolio project with one administrator, a one-time manual DB update is the correct approach. In production, this would be handled by a seed script or a secured internal tool.

---

### 25. Ownership check in returnBook uses ID comparison — not a DB lookup

**Decision:** `returnBook()` verifies the borrow belongs to the caller with `borrow.getUser().getId().equals(userId)`.

**Why:** The `borrow` entity is already loaded — it contains the full `User` relationship. Calling `userRepository.findById(userId)` just to compare IDs would be a wasted database round-trip. Compare the IDs directly from what you already have. Also: `Long` is an object — `==` compares references, not values. Always use `.equals()` for object equality in Java.

---

### 26. Swagger UI added for interactive API documentation

**Decision:** Added `springdoc-openapi-starter-webmvc-ui` and created `SwaggerConfig.java` with JWT Bearer token support.

**Why:** Without documentation, anyone wanting to use or evaluate the API must read the source code. Swagger generates an interactive UI at `/swagger-ui.html` where endpoints can be browsed and tested directly in the browser using a real JWT token. For a portfolio project, it lets interviewers explore the API without Postman or any setup.

---

## 🧠 Key Concepts — How the Backend Actually Works

### What is a JWT?

When a user logs in, the server needs a way to "remember" them on future requests. But this is a stateless REST API — no sessions. So instead, the server hands the client a **signed piece of paper** that says "this is who you are." That piece of paper is a JWT.

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSJ9.abc123xyz
        HEADER                        PAYLOAD                  SIGNATURE
```

| Part | Contents |
|---|---|
| **Header** | Algorithm used to sign (e.g. HS256) |
| **Payload** | Data — email, role, expiry. Base64 encoded, NOT encrypted |
| **Signature** | Cryptographic hash of header + payload, signed with the server's secret key |

If someone tampers with the payload, the signature breaks. The server detects it instantly.

---

### Authentication vs Authorization

| Concept | Question it answers | Where it lives |
|---|---|---|
| **Authentication** | Who are you? | `JwtAuthenticationFilter` |
| **Authorization** | What are you allowed to do? | `SecurityConfig` + `@PreAuthorize` |

---

### Pessimistic Locking — The Race Condition Problem

```
Thread A reads copiesAvailable = 1  ✅ passes check
Thread B reads copiesAvailable = 1  ✅ passes check (without locking)
Thread A writes copiesAvailable = 0
Thread B writes copiesAvailable = -1  ❌ data corruption
```

With `SELECT ... FOR UPDATE`:
- Thread A locks the row
- Thread B blocks until Thread A commits
- Thread B reads the updated value of 0 and fails the check cleanly ✅

---

### Why DTOs — Not Entities Directly

Entities are the internal database representation. DTOs are what travel over the wire. Returning entities directly exposes your entire database schema — including hashed passwords. DTOs give you full control over the API contract.

---

## 🔒 MVP Feature Checklist

| Feature | Status |
|---|---|
| Register / Login (JWT) | ✅ Done |
| firstName in auth response | ✅ Done |
| Role-based access: USER and ADMIN | ✅ Done |
| Book CRUD (Admin only) | ✅ Done |
| Genre field on Book (nullable String) | ✅ Done |
| coverImageUrl on Book (nullable String) | ✅ Done |
| Public book browsing | ✅ Done |
| Borrow a book / Return a book | ✅ Done |
| Borrow status tracking (ACTIVE / RETURNED) | ✅ Done |
| Business rules: max 3 borrows, no duplicates, no copies | ✅ Done |
| Pessimistic locking on borrow | ✅ Done |
| Global exception handling + validation | ✅ Done |
| Two borrow DTOs (user-facing + admin-facing) | ✅ Done |
| UserResponseDTO (no password leak) | ✅ Done |
| Admin user management (list, suspend/unsuspend, delete) | ✅ Done |
| All endpoints Postman tested | ✅ Done |
| Swagger API documentation | ✅ Done |
| Docker Compose for deployment | 📅 Day 18 |
| Deployed to free hosting platform | 📅 Day 19 |
| Basic CI/CD pipeline (GitHub Actions) | 📅 Day 18 |
| React + Vite frontend (Rivendell Reads) | ✅ Done |
| README | 📅 Day 20 |

---

_Last updated: Day 17 ✅ — Backend 100% complete. Frontend feature-complete._