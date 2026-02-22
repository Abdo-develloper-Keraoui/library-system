> The actual spec we're building. Ignore any course material — follow this.

---

## 1. Overview

A fullstack Library Management System built as a 3-week portfolio project for backend-focused internship applications. Prioritizes clean backend architecture, real business logic, and deployment over frontend polish.

**Current status: Day 11 complete — backend feature-complete ✅**

---

## 2. Functional Requirements

### Regular users can:

- Register and login
- Browse the book catalog (public — no login required)
- View book details (public)
- Borrow a book (authenticated only)
- View their borrowing history (authenticated only)
- Return a book (authenticated only)

### Admins can:

- Everything a user can do
- Add, edit, and delete books
- View all borrow records (with borrower details)

### What's NOT included and why:

|Cut Feature|Reason|
|---|---|
|Author as separate entity|Author is a String field on Book. Saves 4–6 hours, no new concepts.|
|OVERDUE borrow status|Requires scheduled jobs or calculation logic. Disproportionate complexity.|

---

## 3. Technical Requirements

|Requirement|Details|
|---|---|
|**Backend**|Spring Boot 4.0.x, Java 21 LTS|
|**Database**|PostgreSQL 17 (Docker container, host port **5555**)|
|**Authentication**|JWT (register + login) via Spring Security|
|**Authorization**|Role-based (USER, ADMIN)|
|**API Documentation**|Swagger / OpenAPI|
|**Validation**|Jakarta Validation on all endpoints|
|**Error Handling**|Global exception handler with consistent error format|
|**Concurrency**|Pessimistic locking on borrow operations|
|**Frontend**|React + Vite (minimal, functional UI)|
|**Frontend Auth**|JWT stored client-side, interceptor for auto-attach, route guards|
|**Containerization**|Docker Compose (backend + frontend + DB) — deployment phase only|
|**Dev Setup**|Only PostgreSQL in Docker; backend on port **8081**, frontend via npm on port 5173|
|**CI/CD**|GitHub Actions — build + test on push|
|**Version Control**|Git + GitHub with meaningful commits|

---

## 4. Database Design

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
│ created_at  │       │ status       │       │ cover_image_url  │
└─────────────┘       └──────────────┘       │ created_at       │
                                             └──────────────────┘
```

- No `authors` table — author is a `String` field on `books`
- `cover_image_url` — nullable String, no file upload
- Borrow status values: `ACTIVE`, `RETURNED` only

---

## 5. API Endpoints

```
# Auth (public)
POST   /api/v1/auth/register
POST   /api/v1/auth/login

# Books (public read, admin write)
GET    /api/v1/books
GET    /api/v1/books/{id}
POST   /api/v1/books                 # Admin only
PUT    /api/v1/books/{id}            # Admin only
DELETE /api/v1/books/{id}            # Admin only

# Borrows (authenticated)
POST   /api/v1/borrows/{bookId}/borrow    # User — borrow a book
PUT    /api/v1/borrows/{id}/return        # User — return a book
GET    /api/v1/borrows/my                 # User — own borrow history
GET    /api/v1/borrows                    # Admin — all borrow records
```

---

## 6. Business Rules

### Borrowing:

1. Book must have `copiesAvailable > 0`
2. User cannot have more than **3 active borrows**
3. User cannot borrow the **same book twice** if already ACTIVE
4. Borrowing **decreases** `copiesAvailable` by 1
5. Borrow period is **14 days** — sets `dueDate` via `@PrePersist`
6. All borrow/return operations are `@Transactional`
7. Concurrent borrow attempts handled with **pessimistic locking**

### Returning:

1. Only the user who borrowed can return
2. Cannot return an already returned book
3. Returning **increases** `copiesAvailable` by 1
4. Sets `returnDate` and status to `RETURNED`

---

## 7. Project Folder Structure

```
library-system/
├── library-management/        # Spring Boot backend
│   └── src/main/java/com/library/library_management/
│       ├── config/
│       │   ├── SecurityConfig.java       ✅
│       │   ├── CorsConfig.java           📅 Before frontend
│       │   └── SwaggerConfig.java        📅 Day 12
│       ├── security/
│       │   ├── JwtUtils.java             ✅
│       │   ├── JwtAuthenticationFilter.java ✅
│       │   └── CustomUserDetailsService.java ✅
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java ✅
│       │   ├── ResourceNotFoundException.java ✅
│       │   └── BusinessException.java    ✅
│       ├── model/
│       │   ├── User.java                 ✅
│       │   ├── Book.java                 ✅
│       │   ├── Borrow.java               ✅
│       │   ├── Role.java                 ✅
│       │   └── BorrowStatus.java         ✅
│       ├── repository/
│       │   ├── UserRepository.java       ✅
│       │   ├── BookRepository.java       ✅
│       │   └── BorrowRepository.java     ✅
│       ├── dto/
│       │   ├── auth/
│       │   │   ├── RegisterDTO.java      ✅
│       │   │   ├── LoginDTO.java         ✅
│       │   │   └── AuthResponseDTO.java  ✅
│       │   ├── book/
│       │   │   ├── BookCreateDTO.java    ✅
│       │   │   ├── BookUpdateDTO.java    ✅
│       │   │   └── BookResponseDTO.java  ✅
│       │   ├── borrow/
│       │   │   ├── BorrowResponseDTO.java      ✅ (user-facing)
│       │   │   └── AdminBorrowResponseDTO.java ✅ (admin-facing)
│       │   └── ErrorResponse.java        ✅
│       ├── service/
│       │   ├── AuthService.java          ✅
│       │   ├── BookService.java          ✅
│       │   └── BorrowService.java        ✅
│       └── controller/
│           ├── AuthController.java       ✅
│           ├── BookController.java       ✅
│           └── BorrowController.java     ✅
├── library-frontend/                     📅 Week 3
├── docker-compose.yml                    📅 Day 18
├── decisions.md                          ✅
├── spec.md                               ✅
├── backend-guide.md                      ✅
├── frontend-guide.md                     ✅
└── README.md                             ✅

```

---

## 8. 3-Week Roadmap

### WEEK 1 — Backend Foundation ✅

|Day|Focus|Status|
|---|---|---|
|1–2|Project setup, PostgreSQL in Docker, first GET endpoint|✅|
|3–4|Book entity + full CRUD, validation, global error handler|✅|
|5–6|User entity, registration, BCrypt password hashing|✅|
|7|JWT authentication — login returns token, filter validates on every request|✅|

### WEEK 2 — Core Logic + Frontend Start

|Day|Focus|Status|
|---|---|---|
|8–9|Role-based authorization (ADMIN vs USER)|✅|
|10–11|Borrow/return endpoints, business rules, pessimistic locking|✅|
|12|Swagger. Backend feature-complete.|🔄 Next|
|13–14|React + Vite setup, login page calling real API|📅|

### WEEK 3 — Frontend + Deploy

|Day|Focus|Status|
|---|---|---|
|15–16|Book browsing, borrow/return buttons, protected routes|📅|
|17|Admin page: add/edit/delete books|📅|
|18|Docker Compose + GitHub Actions CI/CD|📅|
|19|Deploy to Render/Railway, get live URL|📅|
|20|README, final cleanup, practice explaining project|📅|

---

## 9. What Will Impress Interviewers

- A working Docker setup they can run with one command
- A live deployed URL
- A GitHub README with clear setup instructions
- Swagger documentation they can click through
- Proper error handling (not stacktraces as API responses)
- Business logic beyond simple CRUD (borrow rules, concurrency)
- Clean code with consistent naming
- CI/CD pipeline

## 10. Interview Prep — Key Questions

1. Walk me through the architecture of your project. Why this structure?
2. How does JWT authentication work from login to authenticated API call?
3. What happens if two users try to borrow the last copy at the same time?
4. Why DTOs instead of returning entities directly?
5. Why two separate DTOs for borrow responses (user vs admin)?
6. What is `@Transactional` and when would you use it?
7. Why did you choose author as a String instead of a separate entity?
8. How does Docker Compose work? How do your containers communicate?
9. What was the hardest problem you solved?
10. If you had more time, what would you improve?

---

_Last updated: Day 11 ✅_