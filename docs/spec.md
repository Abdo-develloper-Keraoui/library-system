# Project Specification

Full specification for the Library Management System — requirements, endpoints, business rules, and project structure.

---

## Overview

A full-stack library management system built with Spring Boot, React, and PostgreSQL. The project prioritizes clean backend architecture, real business logic enforcement, and a fully containerized deployment.

**Status: Feature-complete ✅ — backend, frontend, and Docker Compose all complete.**

---

## Functional Requirements

### Regular Users Can

- Register and login
- Browse the book catalogue (public — no login required)
- View book details (public)
- Borrow a book (authenticated only)
- View their borrowing history (authenticated only)
- Return a book (authenticated only)

### Admins Can

- Everything a user can do
- Add, edit, and delete books
- View all borrow records with borrower details
- Suspend and unsuspend users
- Delete users

### Intentional Scope Exclusions

| Excluded Feature | Reason |
|---|---|
| Author as separate entity | Author is a String field on Book — no new architectural concepts, not worth the scope increase |
| OVERDUE borrow status | Requires scheduled jobs or calculation logic — disproportionate complexity |

---

## Technical Requirements

| Requirement | Details |
|---|---|
| **Backend** | Spring Boot 4.0.x, Java 21 LTS |
| **Database** | PostgreSQL 17 (Docker container, host port **5555**) |
| **Authentication** | JWT (register + login) via Spring Security |
| **Authorization** | Role-based (USER, ADMIN) |
| **API Documentation** | Swagger / OpenAPI at `/swagger-ui.html` |
| **Validation** | Jakarta Validation on all endpoints |
| **Error Handling** | Global exception handler with consistent error format |
| **Concurrency** | Pessimistic locking on borrow operations |
| **Frontend** | React + Vite |
| **Frontend Auth** | JWT stored in localStorage, interceptor for auto-attach, three route guard components |
| **Containerization** | Docker Compose (backend + frontend + DB) |
| **CI/CD** | GitHub Actions — build + test on push |

---

## Database Design

```
┌─────────────┐       ┌──────────────┐       ┌──────────────────┐
│    users    │       │   borrows    │       │      books       │
├─────────────┤       ├──────────────┤       ├──────────────────┤
│ id (PK)     │──┐    │ id (PK)      │    ┌──│ id (PK)          │
│ first_name  │  └───>│ user_id (FK) │    │  │ title            │
│ last_name   │       │ book_id (FK) │<───┘  │ author (String)  │
│ email (UQ)  │       │ borrow_date  │       │ isbn             │
│ password    │       │ due_date     │       │ pub_year         │
│ role        │       │ return_date  │       │ copies_available │
│ is_active   │       │ status       │       │ cover_image_url  │
│ created_at  │       └──────────────┘       │ genre            │
└─────────────┘                              │ created_at       │
                                             └──────────────────┘
```

- No `authors` table — author is a `String` field on `books`
- `cover_image_url` — nullable String, no file upload
- `genre` — nullable String, max 50 chars
- Borrow status values: `ACTIVE`, `RETURNED` only

---

## API Endpoints

```
# Auth (public)
POST   /api/v1/auth/register          → 201 { token: null, email, role, firstName }
POST   /api/v1/auth/login             → 200 { token, email, role, firstName }

# Books (public read, admin write)
GET    /api/v1/books                  → 200 List<BookResponseDTO>
GET    /api/v1/books/{id}             → 200 BookResponseDTO
POST   /api/v1/books                  → 201 BookResponseDTO          (Admin only)
PUT    /api/v1/books/{id}             → 200 BookResponseDTO          (Admin only)
DELETE /api/v1/books/{id}             → 204                          (Admin only)

# Borrows (authenticated)
POST   /api/v1/borrows/{bookId}/borrow    → 200 BorrowResponseDTO
PUT    /api/v1/borrows/{id}/return        → 200 BorrowResponseDTO
GET    /api/v1/borrows/my                 → 200 List<BorrowResponseDTO>
GET    /api/v1/borrows                    → 200 List<AdminBorrowResponseDTO>   (Admin only)

# Admin — Users
GET    /api/v1/admin/users            → 200 List<UserResponseDTO>    (Admin only)
PUT    /api/v1/admin/users/{id}/suspend → 200 UserResponseDTO        (Admin only)
DELETE /api/v1/admin/users/{id}       → 204                          (Admin only)
```

---

## Business Rules

### Borrowing

1. Book must have `copiesAvailable > 0`
2. User cannot have more than **3 active borrows**
3. User cannot borrow the **same book twice** if already `ACTIVE`
4. Borrowing **decreases** `copiesAvailable` by 1
5. Borrow period is **14 days** — `dueDate` set via `@PrePersist` using `BORROW_PERIOD_DAYS = 14`
6. All borrow/return operations are `@Transactional`
7. Suspended users cannot borrow
8. Concurrent borrow attempts handled with **pessimistic locking** (`SELECT ... FOR UPDATE`)

### Returning

1. Only the user who borrowed can return
2. Cannot return an already-returned book
3. Returning **increases** `copiesAvailable` by 1
4. Sets `returnDate` and `status = RETURNED`

### Deletion (cascade)

1. Deleting a book first deletes all `borrows` where `book_id` matches — prevents FK violation
2. Deleting a user first deletes all `borrows` where `user_id` matches — prevents FK violation
3. Both use `@Modifying + @Transactional` derived delete methods on `BorrowRepository`

---

## Project Structure

```
library-system/
├── docker-compose.yml
├── .env                          ← never commit this
├── .env.example
├── docs/
│   ├── spec.md
│   ├── decisions.md
│   ├── decisions-frontend.md
│   ├── backend-guide.md
│   ├── frontend-guide.md
│   └── uml.md
├── library-management/           ← Spring Boot backend
│   ├── Dockerfile
│   └── src/main/java/com/library/library_management/
│       ├── config/               ← SecurityConfig, CorsConfig, SwaggerConfig
│       ├── security/             ← JwtUtils, JwtAuthenticationFilter, CustomUserDetailsService
│       ├── exception/            ← GlobalExceptionHandler, ResourceNotFoundException, BusinessException
│       ├── model/                ← User, Book, Borrow, Role, BorrowStatus
│       ├── repository/           ← UserRepository, BookRepository, BorrowRepository
│       ├── dto/                  ← auth/, book/, borrow/, user/, ErrorResponse
│       ├── service/              ← AuthService, BookService, BorrowService, AdminUserService
│       ├── controller/           ← AuthController, BookController, BorrowController, AdminUserController
│       └── DataSeeder.java
└── library-frontend/             ← React + Vite frontend
    ├── Dockerfile
    ├── nginx.conf
    └── src/
        ├── api/
        ├── context/
        ├── components/
        ├── pages/
        └── styles/
```
