# 📚 Library Management System

> A full-stack library management system built as a 3-week portfolio project for backend-focused internship applications.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

---

## What is this?

A full-stack library management system where users can browse books, borrow and return them, and admins can manage the catalogue. Built to demonstrate real backend engineering — not just CRUD, but authentication, role-based access control, business rule enforcement, concurrency handling, and deployment.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.x |
| Database | PostgreSQL 17 |
| Auth | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| Frontend | React + Vite *(Week 3)* |
| Containerization | Docker Compose *(Week 3)* |
| CI/CD | GitHub Actions *(Week 3)* |
| Deployment | Render / Railway *(Week 3)* |

---

## Features

### ✅ Backend — Complete
- User registration with BCrypt password hashing
- JWT authentication — login returns a signed token, validated on every request
- Role-based authorization — ADMIN and USER enforced at both config and method level
- Correct 401 / 403 distinction — unauthenticated vs unauthorized
- Full Book CRUD — admin only for writes, public for reads
- Borrow and return books with full status tracking (ACTIVE / RETURNED)
- Business rules enforced — max 3 active borrows, no duplicate borrows, copies must be available
- Pessimistic locking — `SELECT ... FOR UPDATE` prevents race conditions on last-copy borrows
- Separate response DTOs for users and admins — admins see borrower details, users don't
- Input validation and consistent JSON error responses on all endpoints

### 📅 Planned
- [ ] Swagger / OpenAPI documentation
- [ ] React + Vite frontend
- [ ] Docker Compose for one-command local setup
- [ ] GitHub Actions CI/CD pipeline
- [ ] Cloud deployment (live URL)

---

## Architecture

The backend follows a strict layered architecture — each layer has one job and doesn't cross into another's territory.

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
| **Security** | JWT filter identifies every request; SecurityConfig + @PreAuthorize authorize |

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

---

## Running Locally

### Prerequisites
- Java 21
- Docker Desktop
- Maven

### Steps

**1. Clone the repo**
```bash
git clone https://github.com/Abdo-develloper-Keraoui/library-management-system
cd library-management-system
```

**2. Start the database**
```bash
docker-compose up -d
```
> PostgreSQL runs on port **5555** — Windows/Hyper-V reserves the default 5432 range.

**3. Set the JWT secret**

Mac/Linux:
```bash
export JWT_SECRET=your-secret-key-at-least-32-characters-long
```

Windows (PowerShell):
```powershell
$env:JWT_SECRET="your-secret-key-at-least-32-characters-long"
```

**4. Run the backend**
```bash
cd library-management
./mvnw spring-boot:run
```

API available at: `http://localhost:8081`

---

## Database Schema

```
users                 books                   borrows
─────                 ─────                   ───────
id                    id                      id
first_name            title                   user_id  → FK
last_name             author (String)         book_id  → FK
email (unique)        isbn                    borrow_date
password (BCrypt)     pub_year                due_date
role (USER/ADMIN)     copies_available        return_date (nullable)
created_at            cover_image_url (null)  status (ACTIVE/RETURNED)
                      created_at
```

No `authors` table — author is a plain String on Book. Deliberate decision; see `decisions.md`.

---

## Error Handling

All errors return a consistent JSON format — no raw stacktraces ever reach the client:

```json
{
  "status": 404,
  "message": "Book not found with id: 5",
  "timestamp": "2026-02-22T10:30:00"
}
```

---

## Security

- Passwords hashed with BCrypt — plain text never stored
- JWT secret loaded from environment variable — never in source code
- Tokens expire after 24 hours
- Stateless sessions — no server-side session storage
- **401** — token missing, expired, or tampered
- **403** — valid token, but insufficient role

---

## Project Docs

| File | Contents |
|---|---|
| `decisions.md` | Every major architectural decision and the reasoning behind it |
| `spec.md` | Full project specification — requirements, endpoints, roadmap |
| `backend-guide.md` | How each backend layer works |
| `frontend-guide.md` | Frontend structure, request flows, polish guide |
| `uml.md` | Use case diagram with UML → code mapping |

---

## Borrowing Business Rules

1. Book must have `copiesAvailable > 0`
2. User cannot have more than **3 active borrows** at once
3. User cannot borrow the **same book twice** while it is ACTIVE
4. Borrowing decreases `copiesAvailable` by 1; returning increases it by 1
5. Borrow period is **14 days** — `dueDate` set automatically on creation
6. Only the user who borrowed can return
7. Concurrent borrow attempts handled with **pessimistic locking** — the second transaction waits and reads the updated copy count, preventing negative inventory

---

*Built by Abdottawab KERAOUI — learning Spring Boot and backend development.*
