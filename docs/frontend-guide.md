> Frontend begins Day 13. Status: not started. This doc covers folder structure, how data flows from browser to backend, and what to build for polish.

---

## Folder Structure

```
library-frontend/src/
│
├── api/                        ← ALL HTTP calls live here — nowhere else
│   ├── axiosInstance.js        ← base URL + auto-attach JWT to every request
│   ├── authApi.js              ← register(), login()
│   ├── bookApi.js              ← getBooks(), getBook(), createBook(), updateBook(), deleteBook()
│   └── borrowApi.js            ← borrowBook(), returnBook(), getMyBorrows(), getAllBorrows()
│
├── context/
│   └── AuthContext.jsx         ← stores JWT token + user info, available to the whole app
│
├── components/                 ← reusable UI pieces used across pages
│   ├── Navbar.jsx              ← Login/Register when logged out, username/logout when logged in
│   ├── BookCard.jsx            ← one book as a card (title, author, cover image)
│   └── ProtectedRoute.jsx      ← if not logged in, redirect to /login
│
├── pages/
│   ├── LoginPage.jsx           ← public
│   ├── RegisterPage.jsx        ← public
│   ├── BooksPage.jsx           ← public — lists all books
│   ├── BookDetailPage.jsx      ← public — one book + borrow button if logged in
│   ├── MyBorrowsPage.jsx       ← protected — user's borrow history
│   └── admin/
│       ├── AdminBooksPage.jsx  ← protected + ADMIN — book list with edit/delete
│       └── BookFormPage.jsx    ← protected + ADMIN — create or edit a book
│
├── App.jsx                     ← defines all routes, wraps app in AuthContext
└── main.jsx                    ← entry point
```

No state management library. React Context + useState is enough for this scope.

---

## Key Design Decisions

**Why `axiosInstance.js`?** A single configured axios instance means the base URL and JWT header are set once. Every API call automatically has the right headers — you never manually attach the token in individual files. The response interceptor also handles 401s globally: clear the token and redirect to login.

**Why `AuthContext`?** The JWT token needs to be accessible across the whole app — Navbar, ProtectedRoute, API calls. React Context is the simplest way to share state globally without a library.

**Why `ProtectedRoute`?** Instead of checking "is the user logged in?" in every protected page, you wrap the route once. Clean and reusable.

---

## Request Flows

### Flow 1 — User logs in

```
LoginPage → POST /api/v1/auth/login {email, password}
         ← 200 {token, email, role}
         → store in AuthContext
         → redirect to /books
```

### Flow 2 — Browse books (no login needed)

```
BooksPage → GET /api/v1/books (no Authorization header)
          ← 200 [{id, title, author, copiesAvailable...}]
          → render BookCard for each book
```

### Flow 3 — User borrows a book

```
BookDetailPage → POST /api/v1/borrows/{bookId}/borrow
                 Authorization: Bearer <token>
              ← 200 {id, bookTitle, dueDate, status: ACTIVE}
              → show success message, refresh book detail
```

### Flow 4 — Unauthenticated request hits a protected endpoint

```
→ GET /api/v1/borrows/my (no token)
← 401 Unauthorized
axiosInstance interceptor sees 401
→ clear AuthContext
→ redirect to /login
```

### Flow 5 — Admin creates a book

```
BookFormPage → POST /api/v1/books
               Authorization: Bearer <admin_token>
               {title, author, isbn, pubYear, copiesAvailable, coverImageUrl}
            ← 201 {id, title, author...}
            → redirect to /admin/books
```

---

## The Big Picture — Browser to Database

```
BROWSER                              SERVER                       DATABASE
───────                              ──────                       ────────

React Pages
     │
     ▼
axiosInstance ── HTTP Request ──► JwtAuthFilter  (validates token)
(auto-adds JWT)                         │
                                        ▼
                                   Controller    (routes the request)
                                        │
                                        ▼
                                    Service      (enforces business rules)
                                        │
                                        ▼
                                   Repository    (speaks SQL via JPA)
                                        │
                                        ▼
                                                       PostgreSQL
                                        │                   │
                                        ◄── entity data ────┘
                                        │
                                    Service      (maps entity → DTO)
                                        │
                                   Controller    (serializes to JSON)
                                        │
axiosInstance ◄── HTTP Response ────────│
     │
     ▼
React Pages   (update state → re-render UI)
```

---

## Frontend Polish — What to Build (3 Hours Total)

Ranked by impact-to-effort. Do the top items and stop. A clean demo beats an over-engineered one.

### 1. Cover images with fallback (5 min)

```jsx
<img
  src={book.coverImageUrl || "https://placehold.co/200x300?text=No+Cover"}
  alt={book.title}
  style={{ width: "200px", height: "300px", objectFit: "cover" }}
/>
```

`objectFit: "cover"` forces consistent dimensions regardless of source image size.

### 2. Availability badge + disabled borrow button (10 min)

```jsx
<span style={{ color: book.copiesAvailable > 0 ? 'green' : 'red', fontWeight: 'bold' }}>
  {book.copiesAvailable > 0 ? `${book.copiesAvailable} available` : 'Unavailable'}
</span>

<button
  onClick={() => handleBorrow(book.id)}
  disabled={book.copiesAvailable === 0}
  style={{ opacity: book.copiesAvailable === 0 ? 0.5 : 1 }}
>
  {book.copiesAvailable > 0 ? "Borrow" : "Unavailable"}
</button>
```

Disabling the button client-side saves a round trip and directly visualises your `copiesAvailable` business rule.

### 3. Status badges in borrow history (10 min)

```jsx
<span style={{
  backgroundColor: borrow.status === 'ACTIVE' ? '#22c55e' : '#9ca3af',
  color: 'white',
  padding: '2px 10px',
  borderRadius: '999px',
  fontSize: '12px',
  fontWeight: 'bold'
}}>
  {borrow.status}
</span>
```

Green for ACTIVE, grey for RETURNED. Directly visualises your `BorrowStatus` enum.

### 4. Loading states on buttons (15 min)

```jsx
const [loading, setLoading] = useState(false);

const handleBorrow = async (bookId) => {
  setLoading(true);
  try {
    await borrowApi.borrowBook(bookId);
  } catch (error) {
    // show error
  } finally {
    setLoading(false); // always reset — even on failure
  }
};

<button onClick={() => handleBorrow(book.id)} disabled={loading}>
  {loading ? "Borrowing..." : "Borrow Book"}
</button>
```

`finally` is critical — without it a failed request freezes the button permanently.

Interview angle: "Loading states prevent duplicate borrow requests while one is in flight — same principle as pessimistic locking on the backend, just at the UI layer."

### 5. Empty states (10 min)

```jsx
{borrows.length === 0 ? (
  <div style={{ textAlign: 'center', padding: '40px', color: '#6b7280' }}>
    <p style={{ fontSize: '48px' }}>📭</p>
    <p>You haven't borrowed any books yet.</p>
    <a href="/books">Browse the catalogue →</a>
  </div>
) : (
  // render borrow table
)}
```

Makes the app feel complete rather than broken when lists are empty.

---

_Last updated: Day 11 — frontend begins Day 13._