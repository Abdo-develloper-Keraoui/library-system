> Use case diagram — actors, use cases, and relationships. Use this when preparing to explain your system in interviews. Updated through Day 11 — all backend use cases implemented.

---

## The Actors

|Actor|Who they are|In your code|
|---|---|---|
|**Guest**|Unauthenticated visitor|No token → `permitAll()` in SecurityConfig|
|**User**|Logged-in member|JWT with `role = USER`|
|**Admin**|Library administrator|JWT with `role = ADMIN`|

Actors inherit — Admin can do everything User can, User can do everything Guest can:

```
        ┌─────────┐
        │  Guest  │    ← browse, register
        └────┬────┘
             ↑
        ┌────┴────┐
        │  User   │    ← everything Guest can + borrow/return
        └────┬────┘
             ↑
        ┌────┴────┐
        │  Admin  │    ← everything User can + manage books + view all borrows
        └─────────┘
```

In Spring Security: `hasRole("USER")` passes for both USER and ADMIN. `hasRole("ADMIN")` only passes for ADMIN.

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
│                                                                  │
│  ── Internal (support cases) ─────────────────────────────────   │
│  ········ [Authenticate] ············                            │
│  ········ [Verify credentials] ······                            │
│  ········ [Check availability] ······                            │
│  ········ [Check borrow limit] ······                            │
│  ········ [Check no duplicate borrow] ·                          │
│  ········ [Verify ownership] ········                            │
└──────────────────────────────────────────────────────────────────┘
```

Internal (dotted) cases are never triggered directly by an actor — they exist to support other cases via `«include»`.

---

## Relationships

### `«include»` — mandatory, always executes

If the included case fails, the parent case fails.

**Borrow a book:**

```
[Borrow a book] ──«include»──► [Authenticate]
[Borrow a book] ──«include»──► [Check availability]       copiesAvailable > 0
[Borrow a book] ──«include»──► [Check borrow limit]       user has < 3 active borrows
[Borrow a book] ──«include»──► [Check no duplicate borrow]
```

**Return a book:**

```
[Return a book] ──«include»──► [Authenticate]
[Return a book] ──«include»──► [Verify ownership]         this borrow belongs to YOU
```

**Login:**

```
[Login] ──«include»──► [Verify credentials]               BCrypt hash check
```

---

### `«extend»` — optional, only executes under a condition

**Pessimistic locking** — only activates when two requests arrive simultaneously:

```
[Lock book row] ──«extend»──► [Borrow a book]
Condition: {concurrent borrow attempt detected}
```

---

## The Full Diagram

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
└───────┘        │  [Delete book]                                              │
                 │  [View all borrows with borrower details]                   │
                 └─────────────────────────────────────────────────────────────┘
```

---

## UML → Code Mapping

|UML Concept|In your code|
|---|---|
|Actor: Guest|No token → `permitAll()` in `SecurityConfig`|
|Actor: User|JWT with `role = USER`|
|Actor: Admin|JWT with `role = ADMIN`|
|Actor inheritance|`hasRole("ADMIN")` also passes USER-level checks|
|`«include»` Authenticate|`JwtAuthenticationFilter` — runs on every protected request|
|`«include»` Verify credentials|`passwordEncoder.matches(raw, hash)` in `AuthService.login()`|
|`«include»` Check availability|`if (book.getCopiesAvailable() <= 0) throw BusinessException`|
|`«include»` Check borrow limit|`if (activeBorrows >= 3) throw BusinessException`|
|`«include»` Check no duplicate|`existsByUserIdAndBookIdAndStatus()` in `BorrowRepository`|
|`«include»` Verify ownership|`if (!borrow.getUser().getId().equals(userId)) throw BusinessException`|
|`«extend»` Lock row|`@Lock(LockModeType.PESSIMISTIC_WRITE)` on `findByIdForUpdate()`|
|Postcondition: borrow|`copiesAvailable - 1`, `status = ACTIVE`, `@Transactional`|
|Postcondition: return|`copiesAvailable + 1`, `status = RETURNED`, `returnDate = now`, `@Transactional`|
|Admin borrow view|`AdminBorrowResponseDTO` — includes userId, name, email of borrower|

---

## Interview — How to Explain This

**"Can you walk me through your system's use cases?"**

> "There are three actors — Guest, User, and Admin, in an inheritance hierarchy. Guests can browse books and register. Users inherit that and can borrow and return books. Admins inherit from User and can manage the book catalogue and see all borrow records.
> 
> For borrowing, I modelled four mandatory «include» relationships — the operation always checks authentication, availability, the three-borrow limit, and whether the user already has this book active. Concurrent borrow attempts are handled with pessimistic locking, which I modelled as an «extend» since it only activates under a race condition.
> 
> The admin borrow view returns a different DTO from the user borrow view — it includes borrower details like name and email, since admins need to see who borrowed what."

---

_Last updated: Day 11 ✅_