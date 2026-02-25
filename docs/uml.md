# Use Case Diagram

Actors, use cases, and relationships for the Library Management System.

---

## Actors

| Actor | Description | In Code |
|---|---|---|
| **Guest** | Unauthenticated visitor | No token → `permitAll()` in `SecurityConfig` |
| **User** | Logged-in member | JWT with `role = USER` |
| **Admin** | Library administrator | JWT with `role = ADMIN` |

Actors inherit — Admin can do everything User can; User can do everything Guest can:

```
        ┌─────────┐
        │  Guest  │    browse, register, login
        └────┬────┘
             ↑
        ┌────┴────┐
        │  User   │    everything Guest can + borrow/return/history
        └────┬────┘
             ↑
        ┌────┴────┐
        │  Admin  │    everything User can + manage books + manage users + view all borrows
        └─────────┘
```

In Spring Security: `hasRole("USER")` passes for both USER and ADMIN. `hasRole("ADMIN")` passes for ADMIN only.

---

## Use Cases

```
┌──────────────────────────────────────────────────────────────────┐
│                   Library Management System                      │
│                                                                  │
│  ── Available to GUEST ───────────────────────────────────────   │
│  [Browse books]              [View book detail]                  │
│  [Register]                  [Login]                             │
│                                                                  │
│  ── Available to USER ────────────────────────────────────────   │
│  [Borrow a book]             [Return a book]                     │
│  [View my borrow history]                                        │
│                                                                  │
│  ── Available to ADMIN ───────────────────────────────────────   │
│  [Create book]   [Update book]   [Delete book]                   │
│  [View all borrows with borrower details]                        │
│  [Suspend/Unsuspend user]    [Delete user]                       │
│                                                                  │
│  ── Internal (support cases) ─────────────────────────────────   │
│  ········ [Authenticate] ············                            │
│  ········ [Verify credentials] ······                            │
│  ········ [Check account not suspended] ·                        │
│  ········ [Check availability] ······                            │
│  ········ [Check borrow limit] ······                            │
│  ········ [Check no duplicate borrow] ·                          │
│  ········ [Verify ownership] ········                            │
│  ········ [Cascade delete borrows] ··                            │
└──────────────────────────────────────────────────────────────────┘
```

Internal cases (dotted) are never triggered directly by an actor — they exist to support other cases via `«include»`.

---

## Relationships

### `«include»` — mandatory, always executes

If the included case fails, the parent case fails.

**Borrow a book** — checks run in this order in `BorrowService.borrowBook()`:

```
[Borrow a book] ──«include»──► [Authenticate]
[Borrow a book] ──«include»──► [Check availability]             copiesAvailable > 0
[Borrow a book] ──«include»──► [Check borrow limit]             user has < 3 active borrows
[Borrow a book] ──«include»──► [Check no duplicate borrow]
[Borrow a book] ──«include»──► [Check account not suspended]    user.isActive() must be true
```

Note: `[Check account not suspended]` is the last guard checked before creating the borrow record — it comes after the book lock and the three business rule checks. A suspended user who also triggers a different rule will see that rule's error first.

**Return a book:**
```
[Return a book] ──«include»──► [Authenticate]
[Return a book] ──«include»──► [Verify ownership]    this borrow belongs to the caller
```

**Login:**
```
[Login] ──«include»──► [Verify credentials]          BCrypt hash check
```

**Delete book:**
```
[Delete book] ──«include»──► [Cascade delete borrows]    deleteByBookId() before deleteById()
```

**Delete user:**
```
[Delete user] ──«include»──► [Cascade delete borrows]    deleteByUserId() before deleteById()
```

---

### `«extend»` — optional, only activates under a condition

```
[Lock book row] ──«extend»──► [Borrow a book]
Condition: {concurrent borrow attempt detected}
```

---

## Full Diagram

```
                 ┌─────────────────────────────────────────────────────────────┐
                 │                 Library Management System                   │
                 │                                                             │
┌───────┐        │  [Browse books]                                             │
│ Guest ├────────┤  [View book detail]                                         │
└───────┘        │  [Register]                                                 │
    ↑            │  [Login] ──────────────────«include»──► [Verify credentials]│
    │            │                                                             │
┌───────┐        │  [Borrow a book] ──────────«include»──► [Authenticate]      │
│ User  ├────────┤        │ ─────────────────«include»──► [Check availability] │
└───────┘        │        │ ─────────────────«include»──► [Check borrow limit] │
    ↑            │        │ ─────────────────«include»──► [Check no duplicate] │
    │            │        │ ─────────────────«include»──► [Check not suspended]│
    │            │        ↑                                                    │
    │            │  [Lock book row] ──────────«extend»── {concurrent request}  │
    │            │                                                             │
    │            │  [Return a book] ──────────«include»──► [Authenticate]      │
    │            │        │ ─────────────────«include»──► [Verify ownership]   │
    │            │                                                             │
    │            │  [View my borrow history]                                   │
    │            │                                                             │
┌───────┐        │  [Create book]                                              │
│ Admin ├────────┤  [Update book]                                              │
└───────┘        │  [Delete book] ────────────«include»──► [Cascade delete borrows]│
                 │  [View all borrows with borrower details]                   │
                 │  [Suspend/Unsuspend user]                                   │
                 │  [Delete user] ────────────«include»──► [Cascade delete borrows]│
                 └─────────────────────────────────────────────────────────────┘
```

---

## UML → Code Mapping

| UML Concept | In Code |
|---|---|
| Actor: Guest | No token → `permitAll()` in `SecurityConfig` |
| Actor: User | JWT with `role = USER` |
| Actor: Admin | JWT with `role = ADMIN` |
| Actor inheritance | `hasRole("ADMIN")` also passes USER-level checks |
| `«include»` Authenticate | `JwtAuthenticationFilter` — runs on every protected request |
| `«include»` Verify credentials | `passwordEncoder.matches(raw, hash)` in `AuthService.login()` |
| `«include»` Check availability | `if (book.getCopiesAvailable() <= 0) throw BusinessException` in `BorrowService` |
| `«include»` Check borrow limit | `if (borrowRepository.countByUserIdAndStatus(...) >= 3) throw BusinessException` |
| `«include»` Check no duplicate | `existsByUserIdAndBookIdAndStatus()` in `BorrowRepository` |
| `«include»` Check not suspended | `if (!user.isActive()) throw BusinessException(...)` in `BorrowService` |
| `«include»` Verify ownership | `if (!borrow.getUser().getId().equals(userId)) throw BusinessException` — `.equals()` not `==` because Long is an object |
| `«include»` Cascade delete borrows | `borrowRepository.deleteByBookId(id)` / `deleteByUserId(id)` — `@Modifying @Transactional` |
| `«extend»` Lock row | `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `findByIdForUpdate()` in `BookRepository` |
| Postcondition: borrow | `copiesAvailable - 1`, `status = ACTIVE`, `@Transactional` |
| Postcondition: return | `copiesAvailable + 1`, `status = RETURNED`, `returnDate = LocalDate.now()`, `@Transactional` |
| Admin borrow view | `AdminBorrowResponseDTO` — includes `userId`, `userFirstName`, `userLastName`, `userEmail` |
| Admin suspend/unsuspend | `adminUserService.suspendUser(userId)` — flips `user.setActive(!user.isActive())` |
| Admin delete user | `adminUserService.deleteUser(id)` — cascade borrows then hard delete |
