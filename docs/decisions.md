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

### 6. Admin user management: suspend and delete — added in backend

**Decision:** Three new admin endpoints — list all users, suspend/unsuspend a user (`isActive` toggle), delete a user. `User` entity has an `isActive` boolean field defaulting to `true`.

**Why:** Demonstrates admin power beyond book management. Suspend is more realistic than hard-delete for most violations — an admin can revoke borrowing access without losing the user's history. Delete is also available for full cleanup. Both are ADMIN-only and locked down in `SecurityConfig`.

---

## 🛠️ Technology Decisions

### 7. React + Vite for frontend — not Next.js

**Decision:** Frontend is built with plain React + Vite.

**Why:** Zero JavaScript experience going in. Next.js adds SSR, file-based routing, and its own abstractions — three layers of new concepts at once. React + Vite means only learning components, state, and API calls. The frontend is ~20% of this project; the backend is where the interview value lives.

---

### 8. Spring Security + JWT authentication

**Decision:** Authentication uses Spring Security with JWT tokens.

**Why:** In the Java ecosystem, Spring Security + JWT is the industry standard. This is what recruiters and interviewers expect to see on a Java backend project.

---

### 9. Only PostgreSQL runs in Docker during development

**Decision:** PostgreSQL runs in Docker. Spring Boot runs from IntelliJ. React runs via `npm run dev`.

**Why:** Fast feedback loops during development. Only the database is infrastructure — it belongs in a container. Everything gets containerized at deployment. This mirrors how real development teams work.

---

### 10. Full Docker Compose setup is a deployment-phase task

**Decision:** Dockerfiles and `docker-compose.yml` are written once the application is feature-complete.

**Why:** Writing Docker configs during active development adds unnecessary build cycles. It's a deployment concern, not a dev concern. Doing it at the end proves we understand containerization without slowing down daily progress.

---

### 11. CI/CD with GitHub Actions — minimal pipeline

**Decision:** A basic GitHub Actions pipeline runs build + tests on every push to `main`.

**Why:** Takes ~1–2 hours, sends a strong signal to recruiters that we understand modern development workflows. Kept simple — no multi-stage deployments or complex configurations.

---

## 🏗️ Architecture Decisions

### 12. Layered architecture: Controller → Service → Repository

**Decision:** Standard three-layer Spring Boot architecture.

**Why:** Controllers handle HTTP. Services handle business logic. Repositories handle data access. Clean separation of concerns makes the code testable, maintainable, and easy to explain. This is what interviewers expect from a Java backend project.

---

### 13. Service methods throw exceptions on "not found" — never return Optional

**Decision:** Services throw `ResourceNotFoundException` instead of returning `Optional`.

**Why:** In a REST API, "not found" is an error — the correct response is 404, not 200 with an empty body. Returning `Optional` pushes the decision to the controller, so every controller must handle it separately. Throwing an exception lets `GlobalExceptionHandler` catch it once and return a consistent 404 everywhere.

---

### 14. Custom exception classes — not generic RuntimeException

**Decision:** `ResourceNotFoundException`, `BusinessException` instead of raw `RuntimeException`.

**Why:** A generic `RuntimeException` gives Spring no information about what went wrong — it defaults to 500 regardless. Custom exceptions carry meaning and let `GlobalExceptionHandler` map each one to the correct HTTP status (404, 400). One place to change, consistent behaviour everywhere.

---

### 15. Password hashing in the service layer — not controller or entity

**Decision:** BCrypt hashing happens in `AuthService`, not `AuthController` or `User`.

**Why:** The service layer is where business logic lives. If hashing were done in the controller, every controller that creates a user would have to remember to hash — that's duplication and a security risk. In the service, it happens once, every time, in one place.

---

### 16. Validation errors handled in GlobalExceptionHandler — not per-controller

**Decision:** `MethodArgumentNotValidException` is caught once in `GlobalExceptionHandler`.

**Why:** Without a central handler, Spring returns its own inconsistent error format. Catching it once means every endpoint returns the same `ErrorResponse` shape on validation failure. One place to change, consistent behaviour everywhere.

---

### 17. Role-based authorization: SecurityConfig for broad rules + @PreAuthorize for fine-grained rules

**Decision:** Authorization uses both `SecurityConfig` and `@PreAuthorize` together.

`SecurityConfig` handles broad structural rules — auth endpoints are public, GET book endpoints are public, everything else requires authentication.

`@PreAuthorize("hasRole('ADMIN')")` handles fine-grained rules directly on controller methods — POST/PUT/DELETE books, GET all borrows, all admin user management endpoints.

**Why:** Putting all rules in `SecurityConfig` creates a hidden coupling problem — a developer reading `BookController.java` has no idea who can call each method without opening a separate file. As the project grows, `SecurityConfig` becomes a fragile list of URL patterns where order matters. `@PreAuthorize` puts the authorization rule at the point of definition, right next to the method it protects. The two layers serve different purposes and work best together. This is the industry-standard approach in Spring Boot projects.

---

### 18. Stateless API with JWT — no server-side sessions

**Decision:** The API uses stateless JWT authentication. No sessions are created or stored on the server.

**Why:** Session-based auth makes the server the source of truth — sticky sessions fail if that server goes down, and a shared session database is a single point of failure. JWT solves this cleanly. The token itself is the session — generated at login, stored client-side, sent with every request. Any server can validate it independently using the shared secret key. No shared state, scales to any number of servers.

---

### 19. JwtAuthenticationFilter identifies every request — never blocks

**Decision:** `JwtAuthenticationFilter` runs on every request. Its only job is identification, not blocking.

**Why:** The filter reads the `Authorization` header, skips silently if no Bearer token is present, validates the token if one exists, and stores the user's identity in `SecurityContextHolder`. It always calls `filterChain.doFilter()` — it never short-circuits. Blocking is the responsibility of `SecurityConfig` and `@PreAuthorize` downstream. Mixing identification and authorization in the filter would violate separation of concerns.

---

### 20. 401 vs 403 handled by separate mechanisms

**Decision:** 401 is returned by the `authenticationEntryPoint` in `SecurityConfig`. 403 is returned automatically by Spring when `@PreAuthorize` fails.

**Why:** They represent fundamentally different failures. 401 means "I don't know who you are" — missing, expired, or tampered token. 403 means "I know exactly who you are, but you're not allowed to do this" — valid token, wrong role. Keeping them separate means each failure returns the correct semantic HTTP status.

---

### 21. No BorrowRequestDTO — bookId comes from URL path, userId from JWT

**Decision:** `POST /api/v1/borrows/{bookId}/borrow` takes `bookId` as a path variable. `userId` is extracted from the JWT via `@AuthenticationPrincipal`. No request body, no input DTO needed.

**Why:** DTOs exist to represent what travels over the wire in the request body. When the only inputs are a path variable and a JWT-derived userId, there is nothing to deserialize. Creating a DTO here would be unnecessary ceremony with no benefit.

---

### 22. @PrePersist handles borrowDate, dueDate, and status — not the service

**Decision:** `borrowDate`, `dueDate` (borrowDate + 14 days), and initial `status` (ACTIVE) are set automatically in `Borrow.java` via `@PrePersist`. The service only sets `user` and `book`.

**Why:** These values are always the same at creation time — they are not decisions the service needs to make. Putting them in `@PrePersist` guarantees they are always set correctly regardless of how a `Borrow` is created, and keeps the service focused on business logic rather than entity initialization.

---

### 23. Pessimistic locking on borrow operations

**Decision:** `BookRepository.findByIdForUpdate()` uses `@Lock(PESSIMISTIC_WRITE)`, issuing `SELECT ... FOR UPDATE` inside the borrow transaction.

**Why:** Prevents two users from borrowing the last copy simultaneously. Without locking, both threads could read `copiesAvailable = 1`, both pass the check, and both decrement — leaving the count at -1. The lock serializes access: the second transaction waits until the first commits. Pessimistic locking is the simplest correct solution for this problem at this scale.

---

### 24. Two separate borrow response DTOs — one for users, one for admins

**Decision:** `BorrowResponseDTO` is returned to users from `GET /api/v1/borrows/my`. `AdminBorrowResponseDTO` is returned to admins from `GET /api/v1/borrows`. The admin DTO includes additional fields: `userId`, `userFirstName`, `userLastName`, `userEmail`. `BorrowService` has two corresponding private mapping methods: `mapToDTO()` and `mapToAdminDTO()`.

**Why:** A regular user calling `GET /borrows/my` has no need for their own name and email echoed back — they already know who they are. An admin calling `GET /borrows` needs to know _who_ borrowed _what_, so borrower identity is essential. The endpoints are protected differently at the controller level, so there is no leakage risk. Two DTOs, two mappers, clean separation of concerns.

---

### 25. Controller extracts userId from JWT — service receives a plain Long

**Decision:** `BorrowController` resolves the authenticated user via `@AuthenticationPrincipal` and passes `userId` (a `Long`) to the service. The service never touches `SecurityContextHolder` or `UserDetails`.

**Why:** The service layer should be agnostic of HTTP and security concerns. If the service reached into `SecurityContextHolder` directly, it would be tightly coupled to Spring Security — impossible to unit test without a security context, and the service would be doing two jobs. The controller's job is to translate HTTP inputs (including JWT identity) into plain Java values. The service's job is business logic. Each layer does one thing.

---

### 26. Ownership check in returnBook uses ID comparison — not a DB lookup

**Decision:** `returnBook()` verifies the borrow belongs to the caller with `borrow.getUser().getId().equals(userId)`. No additional database query.

**Why:** The `borrow` entity is already loaded — it contains the full `User` relationship. Calling `userRepository.findById(userId)` to get a `User` object just to compare IDs would be a wasted round-trip to the database. Compare the IDs directly from what you already have. Also: `Long` is an object — `==` compares references, not values. Always use `.equals()` for object equality in Java.

---

### 27. Swagger UI added for interactive API documentation

**Decision:** Added `springdoc-openapi-starter-webmvc-ui` and created `SwaggerConfig.java` with JWT Bearer token support. Swagger URLs whitelisted in `SecurityConfig`.

**Why:** Without documentation, anyone wanting to use or evaluate the API must read the source code. Swagger generates an interactive UI at `/swagger-ui.html` where endpoints can be browsed and tested directly in the browser using a real JWT token. For a portfolio project, it lets interviewers explore the API without Postman or any setup.

---

## 🧠 Key Concepts — How the Backend Actually Works

### What is a JWT?

When a user logs in, the server needs a way to "remember" them on future requests. But this is a stateless REST API — no sessions. So instead, the server hands the client a **signed piece of paper** that says "this is who you are." That piece of paper is a JWT — JSON Web Token.

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSJ9.abc123xyz
        HEADER                        PAYLOAD                  SIGNATURE
```

|Part|Contents|
|---|---|
|**Header**|Algorithm used to sign (e.g. HS256)|
|**Payload**|Data — email, role, expiry. Base64 encoded, NOT encrypted — anyone can read it|
|**Signature**|Cryptographic hash of header + payload, signed with the server's secret key|

**The signature is the key insight.** If someone tampers with the payload, the signature breaks. The server detects it instantly. The secret key never touches the codebase — it lives in `application.properties` as `${JWT_SECRET}` and is injected via environment variables.

---

### The Full JWT Request Lifecycle

```
1. POST /api/v1/auth/login  {email, password}
         ↓
2. AuthService verifies password with BCrypt
         ↓
3. JwtUtils generates a signed token
         ↓
4. Server returns: { "token": "eyJhbG..." }
         ↓
5. Client stores the token
         ↓
6. Every subsequent request sends:
   Authorization: Bearer eyJhbG...
         ↓
7. JwtAuthenticationFilter reads + validates the token,
   extracts email, loads user, sets SecurityContextHolder
         ↓
8. Controller accesses the user via @AuthenticationPrincipal
         ↓
9. SecurityConfig / @PreAuthorize allows or denies based on role
```

---

### Authentication vs Authorization

|Concept|Question it answers|Where it lives|
|---|---|---|
|**Authentication**|Who are you?|`JwtAuthenticationFilter`|
|**Authorization**|What are you allowed to do?|`SecurityConfig` + `@PreAuthorize`|

The filter identifies. The config decides. They are separate concerns.

---

### Pessimistic Locking — The Race Condition Problem

The borrow operation has a race condition. When two users hit `POST /borrows/42/borrow` at the exact same moment and Book 42 has 1 copy left:

```
Thread A reads copiesAvailable → 1  ✅ passes check
Thread B reads copiesAvailable → 1  ✅ passes check

Thread A creates borrow, writes copiesAvailable = 0
Thread B creates borrow, writes copiesAvailable = -1  ❌
```

**Pessimistic locking is the database equivalent of a mutex.** `SELECT ... FOR UPDATE` places an exclusive lock on the row. Any other transaction that tries to read or write that row blocks until the first transaction commits.

```
Thread A                            Thread B
   |                                   |
   | findByIdForUpdate(42)             |
   | → DB locks row 42 🔒              |
   | copiesAvailable = 1               |   findByIdForUpdate(42)
   | ✅ passes check                   |    ⏳ BLOCKED — waiting for lock
   | creates borrow record             |    ⏳ ...still waiting...
   | copiesAvailable → 0               |    ⏳ ...still waiting...
   | @Transactional commits 🔓         |
   |                                   | → gets the lock 🔒
   |                                   | reads copiesAvailable = 0
   |                                   | ❌ fails check → throws BusinessException
   |                                   | rolls back 🔓
```

Thread B now reads the updated value of 0. Data integrity preserved.

---

### @PrePersist — Automatic Field Initialization

`@PrePersist` is a JPA lifecycle hook. It runs automatically just before a new entity is saved to the database for the first time. In `Borrow.java` it sets `borrowDate` (today), `dueDate` (today + 14 days), and `status` (ACTIVE) — the service never needs to think about these.

---

### Why DTOs — Not Entities Directly

Entities are the internal database representation. DTOs are what travel over the wire. Returning entities directly exposes your entire database schema — including hashed passwords and internal audit fields. DTOs give you full control over the API contract.

---

### @AuthenticationPrincipal — How Controllers Access the Current User

After `JwtAuthenticationFilter` validates the token and stores the user in `SecurityContextHolder`, any controller method can inject the authenticated user directly using `@AuthenticationPrincipal UserDetails userDetails`. Spring resolves this automatically. `userDetails.getUsername()` returns the email. From the email, the controller calls `userRepository.findByEmail()` to get the `userId` and passes it to the service.

---

## 🔒 MVP Feature Checklist

|Feature|Status|
|---|---|
|Register / Login (JWT)|✅ Done|
|Role-based access: USER and ADMIN|✅ Done|
|Book CRUD (Admin only)|✅ Done|
|Genre field on Book|✅ Done|
|Public book browsing|✅ Done|
|Borrow a book / Return a book|✅ Done|
|Borrow status tracking (ACTIVE / RETURNED)|✅ Done|
|Business rules: max 3 borrows, no duplicates, no copies|✅ Done|
|Pessimistic locking on borrow|✅ Done|
|Global exception handling + validation|✅ Done|
|Two borrow DTOs (user-facing + admin-facing)|✅ Done|
|Admin user management (list, suspend, delete)|🔄 In progress|
|All endpoints Postman tested|✅ Done|
|Swagger API documentation|✅ Done|
|Docker Compose for deployment|📅 Day 18|
|Deployed to free hosting platform (live URL)|📅 Day 19|
|Basic CI/CD pipeline (GitHub Actions)|📅 Day 18|
|React + Vite frontend|🔄 In progress (Day 13–17)|
|README|✅ In progress|

---

_Last updated: Day 14 ✅ — Genre added. Admin user management in progress._