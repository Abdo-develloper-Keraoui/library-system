# 📚 Rivendell Reads — Library Management System

> A full-stack library management system with JWT authentication, role-based access control, borrow business logic, pessimistic concurrency handling, and a full Docker Compose deployment setup.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.x-brightgreen)
![React](https://img.shields.io/badge/React-Vite-61DAFB)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![CI](https://github.com/Abdo-develloper-Keraoui/library-system/actions/workflows/ci.yml/badge.svg)
![Status](https://img.shields.io/badge/Status-Live-brightgreen)

---

## 🌐 Live Demo

| | URL |
|---|---|
| **Frontend** | [https://rivendell-reads.netlify.app](https://rivendell-reads.netlify.app) |
| **Backend API** | [https://library-backend-t2y8.onrender.com/api/v1/books](https://library-backend-t2y8.onrender.com/api/v1/books) |
| **Swagger UI** | [https://library-backend-t2y8.onrender.com/swagger-ui.html](https://library-backend-t2y8.onrender.com/swagger-ui.html) |

**Demo accounts:**
| Role | Email | Password |
|---|---|---|
| Admin | `Ahmed@google.com` | `Password123` |
| User | `Ahmed@User.com` | `Password123` |

> ⚠️ The backend runs on Render's free tier and may take 30–60 seconds to wake up on the first request.

---

## What Is This?

A full-stack library management system where users can browse books, borrow and return them, and admins can manage the catalogue and user accounts. The project prioritizes clean backend architecture, real business logic enforcement, and a fully containerized deployment over frontend complexity.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.x |
| Database | PostgreSQL 17 |
| Auth | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| API Docs | Swagger / OpenAPI (`/swagger-ui.html`) |
| Frontend | React + Vite |
| Styling | Plain CSS with a custom design system |
| Containerization | Docker Compose |
| CI/CD | GitHub Actions |
| Deployment | Render (backend + DB) + Netlify (frontend) |

---

## Features

### Backend
- User registration with BCrypt password hashing
- JWT authentication — login returns a signed token, validated on every request via a filter
- Role-based authorization — `ADMIN` and `USER` enforced at both `SecurityConfig` and `@PreAuthorize` level
- Correct 401 / 403 distinction — unauthenticated vs unauthorized
- Full Book CRUD — admin-only for writes, public for reads
- Borrow and return flow with full status tracking (`ACTIVE` / `RETURNED`)
- Business rules enforced: max 3 active borrows per user, no duplicate active borrows, copies must be available
- Pessimistic locking — `SELECT ... FOR UPDATE` prevents race conditions on last-copy borrow attempts
- Separate response DTOs for users and admins — admins see borrower details, users see only their own data
- Consistent JSON error responses via `GlobalExceptionHandler` — no raw stacktraces
- Input validation on all endpoints (Jakarta Validation)
- Swagger UI for interactive API exploration

### Frontend (Rivendell Reads)
- Tolkien-inspired design system — custom CSS variables, Cinzel/Merriweather/Lato fonts
- Animated homepage with scrolling book belt populated from live catalogue data
- Book browsing with client-side genre filter
- Borrow and return books inline, with live availability tracking
- My Borrows history page
- Admin book management — create, edit, delete with inline confirmation
- Admin user management — suspend/unsuspend, delete with inline confirmation
- Three route guard types: `GuestRoute`, `ProtectedRoute`, `AdminRoute`
- Centralized API layer (`src/api/`) with Axios and auto-attached JWT interceptor

---

## Quick Start — Docker (Recommended)

### Prerequisites
- Docker Desktop

### Steps

**1. Clone the repo**
```bash
git clone https://github.com/Abdo-develloper-Keraoui/library-system.git
cd library-system
```

**2. Create the `.env` file**
```bash
cp .env.example .env
```
Then edit `.env` and set a real JWT secret (min 32 characters):
```
JWT_SECRET=your_real_secret_here_at_least_32_chars
JWT_EXPIRATION=86400000
```

**3. Run**
```bash
docker compose up --build
```

App is available at **`http://localhost:5173`**

The database is seeded automatically on first startup with:
- Admin: `Ahmed@google.com` / `admin123`
- User: `Ahmed@User.com` / `user123`
- 33 books with cover images

To wipe data and reseed from scratch:
```bash
docker compose down -v
docker compose up --build
```

---

## Local Development Setup

Run only PostgreSQL in Docker; run the backend and frontend natively for fast feedback loops.

### Prerequisites
- Java 21
- Docker Desktop
- Node.js 18+
- Maven

### 1. Start the database
```bash
docker compose up postgres -d
```
PostgreSQL runs on port **5555**.

### 2. Run the backend
```bash
cd library-management
export JWT_SECRET=your-secret-key-at-least-32-characters-long  # Mac/Linux
# $env:JWT_SECRET="your-secret-key..."                          # Windows PowerShell
./mvnw spring-boot:run
```
API available at `http://localhost:8081`
Swagger UI at `http://localhost:8081/swagger-ui.html`

### 3. Run the frontend
```bash
cd library-frontend
npm install
npm run dev
```
App available at `http://localhost:5173`

---

## Architecture

```
Request → JwtAuthFilter → Controller → Service → Repository → PostgreSQL
                                          ↑
                               Business rules enforced here
```

| Layer | Responsibility |
|---|---|
| **Controller** | HTTP routing only — no business logic |
| **Service** | All business rules and logic |
| **Repository** | Database access via Spring Data JPA |
| **DTOs** | Separate input/output shapes — entities never exposed directly |
| **Security** | JWT filter identifies every request; `SecurityConfig` + `@PreAuthorize` authorize |

---

## API Reference

| Method | Endpoint | Access |
|--------|----------|--------|
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| GET | `/api/v1/books` | Public |
| GET | `/api/v1/books/{id}` | Public |
| POST | `/api/v1/books` | Admin only |
| PUT | `/api/v1/books/{id}` | Admin only |
| DELETE | `/api/v1/books/{id}` | Admin only |
| POST | `/api/v1/borrows/{bookId}/borrow` | Authenticated |
| PUT | `/api/v1/borrows/{id}/return` | Authenticated |
| GET | `/api/v1/borrows/my` | Authenticated |
| GET | `/api/v1/borrows` | Admin only |
| GET | `/api/v1/admin/users` | Admin only |
| PUT | `/api/v1/admin/users/{id}/suspend` | Admin only |
| DELETE | `/api/v1/admin/users/{id}` | Admin only |

Full interactive documentation available at [Swagger UI](https://library-backend-t2y8.onrender.com/swagger-ui.html).

---

## Database Schema

```
users                   books                   borrows
─────                   ─────                   ───────
id                      id                      id
first_name              title                   user_id  → FK
last_name               author (String)         book_id  → FK
email (unique)          isbn                    borrow_date
password (BCrypt)       pub_year                due_date
role (USER/ADMIN)       copies_available        return_date (nullable)
is_active               cover_image_url (null)  status (ACTIVE/RETURNED)
created_at              genre (null)
                        created_at
```

---

## Borrowing Business Rules

1. Book must have `copiesAvailable > 0`
2. User cannot have more than **3 active borrows** at once
3. User cannot borrow the **same book twice** while it is `ACTIVE`
4. Borrowing decreases `copiesAvailable` by 1; returning increases it by 1
5. Borrow period is **14 days** — `dueDate` set automatically via `@PrePersist`
6. Only the user who borrowed can return
7. Suspended users cannot borrow
8. Concurrent borrow attempts handled with **pessimistic locking** — the second transaction waits and reads the updated copy count, preventing negative inventory

---

## Error Handling

All errors return a consistent JSON format — no raw stacktraces ever reach the client:

```json
{
  "status": 404,
  "message": "Book not found with id: 5",
  "timestamp": "2026-02-25T10:30:00"
}
```

---

## Security

- Passwords hashed with BCrypt — plain text never stored
- JWT secret loaded from environment variable — never in source code
- Tokens expire after 24 hours (configurable via `JWT_EXPIRATION`)
- Stateless sessions — no server-side session storage
- **401** — token missing, expired, or tampered
- **403** — valid token, but insufficient role
- `localStorage` used for token storage on the frontend — appropriate for this scope; `httpOnly` cookies would be preferred in a higher-security production environment

---

## Project Structure

```
library-system/
├── .github/workflows/ci.yml      ← GitHub Actions CI pipeline
├── docker-compose.yml
├── .env                          ← never commit this
├── .env.example                  ← safe to commit
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
│       └── DataSeeder.java       ← seeds DB on first startup
└── library-frontend/             ← React + Vite frontend
    ├── Dockerfile
    ├── nginx.conf
    ├── netlify.toml
    └── src/
        ├── api/                  ← axiosInstance, authApi, bookApi, borrowApi, adminApi
        ├── context/              ← AuthContext
        ├── components/           ← Navbar, BookCard, GuestRoute, ProtectedRoute, AdminRoute
        ├── pages/                ← all pages including admin/
        └── styles/               ← global.css (Rivendell Reads design system)
```

---

## Documentation

| File | Contents |
|---|---|
| `docs/spec.md` | Full project specification — requirements, endpoints, business rules |
| `docs/decisions.md` | Backend architectural decisions and the reasoning behind each |
| `docs/decisions-frontend.md` | Frontend technical decisions |
| `docs/backend-guide.md` | How each backend layer works |
| `docs/frontend-guide.md` | Frontend structure, design system, and request flows |
| `docs/uml.md` | Use case diagram with UML → code mapping |

---

*Built by Abdottawab KERAOUI*