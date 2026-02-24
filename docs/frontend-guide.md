> Frontend started Day 13. Updated through Day 15. Design system locked. Auth pages complete.

---

## Folder Structure

```
library-frontend/src/
│
├── api/                        ← ALL HTTP calls live here — nowhere else
│   ├── axiosInstance.js        ✅ base URL + auto-attach JWT
│   ├── authApi.js              ✅ register(), login()
│   ├── bookApi.js              📅 getAllBooks(), getBookById()
│   └── borrowApi.js            📅 borrowBook(), returnBook(), getMyBorrows(), getAllBorrows()
│
├── context/
│   └── AuthContext.jsx         ✅ stores token, email, role, firstName
│
├── components/
│   ├── GuestRoute.jsx          ✅ redirects logged-in users away from /login and /register
│   ├── Navbar.jsx              ✅ Rivendell themed, role-based links, ꧁ name ꧂ pill
│   ├── BookCard.jsx            📅 one book as a themed card
│   └── ProtectedRoute.jsx      📅 redirects unauthenticated users to /login
│
├── pages/
│   ├── LoginPage.jsx           ✅ themed, register link
│   ├── RegisterPage.jsx        ✅ themed, login link
│   ├── BooksPage.jsx           📅 public book grid
│   ├── BookDetailPage.jsx      📅 single book + borrow button
│   ├── MyBorrowsPage.jsx       📅 user borrow history
│   └── admin/
│       ├── AdminBooksPage.jsx  📅 CRUD for books
│       ├── AdminUsersPage.jsx  📅 suspend/delete users
│       └── BookFormPage.jsx    📅 add/edit book form
│
├── styles/
│   └── global.css              ✅ Rivendell Reads design system (see below)
│
├── App.jsx                     ✅ BrowserRouter + AuthProvider + Navbar + routes
└── main.jsx                    ✅ entry point
```

---

## Design System — Rivendell Reads

### Philosophy
Tolkien-inspired. Warm parchment backgrounds, dark brown navbar, jade green accents, muted gold highlights. Distinctive and memorable for a portfolio project.

### Fonts (loaded in index.html via Google Fonts)
```html
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@400;600;700&family=Merriweather:wght@400;700&family=Lato:wght@400;600&display=swap" rel="stylesheet">
```
- `Cinzel` → brand name only
- `Merriweather` → page headings
- `Lato` → body text and buttons

### CSS Variables
```css
--bg-page:      #faf6ef   /* warm parchment */
--bg-card:      #fffcf5   /* lighter parchment for cards */
--bg-section:   #f0e9db   /* darker parchment for page backgrounds */
--bg-navbar:    #2c1f14   /* dark warm brown */

--jade:         #3d7a5c   /* primary buttons */
--jade-hover:   #2d5e45
--gold:         #c49a3c   /* logo, name pill */
--danger:       #b85c3a   /* delete/logout */

--text-heading: #1e1208
--text-body:    #3d2b1a
--text-medium:  #6b5032
--text-light:   #9c866e
--text-navbar:  #e8dcc8

--border:       #ddd0bb
--font-brand:   'Cinzel', serif
--font-heading: 'Merriweather', serif
--font-body:    'Lato', sans-serif
```

### Available CSS classes
`.btn`, `.btn-primary`, `.btn-danger`, `.btn-secondary`, `.btn-secondary-light`, `.card`, `.form-group`, `.badge`, `.badge-green`, `.badge-grey`, `.badge-red`, `.badge-gold`, `.error-msg`, `.success-msg`, `.empty-state`, `.page`, `.container`, `.divider`

### Golden rule
**Never hardcode hex colors in component files. Always use CSS variables.**

---

## Key Design Decisions

**Why `axiosInstance.js`?** Base URL and JWT header set once. Every API call automatically has the right headers.

**Why `AuthContext`?** JWT token + user info needs to be accessible everywhere — Navbar, ProtectedRoute, all pages. Context is the simplest way to share state globally.

**Why `ProtectedRoute`?** Instead of checking "is the user logged in?" in every protected page, wrap the route once.

**Why `GuestRoute`?** Prevents a logged-in user from seeing the login/register pages. Redirects to `/`.

---

## Request Flows

### Flow 1 — User logs in
```
LoginPage → POST /api/v1/auth/login {email, password}
         ← 200 {token, email, role, firstName}
         → loginUser(data) → AuthContext + localStorage
         → navigate('/books')
```

### Flow 2 — Browse books (no login needed)
```
BooksPage → GET /api/v1/books (no Authorization header needed)
          ← 200 [{id, title, author, genre, copiesAvailable, coverImageUrl...}]
          → render BookCard for each book
```

### Flow 3 — User borrows a book
```
BookDetailPage → POST /api/v1/borrows/{bookId}/borrow
                 Authorization: Bearer <token>  ← axiosInstance adds this automatically
              ← 200 {id, bookTitle, dueDate, status: ACTIVE}
              → show success, refresh book detail
```

### Flow 4 — Unauthenticated request hits protected endpoint
```
→ GET /api/v1/borrows/my (no token)
← 401 Unauthorized
→ ProtectedRoute catches this → redirect to /login
```

### Flow 5 — Admin creates a book
```
BookFormPage → POST /api/v1/books
               Authorization: Bearer <admin_token>
            ← 201 {id, title, author...}
            → navigate('/admin/books')
```

---

## Frontend Polish Checklist

### 1. Cover images with fallback
```jsx
<img
  src={book.coverImageUrl || 'https://placehold.co/200x300?text=No+Cover'}
  alt={book.title}
  style={{ width: '200px', height: '300px', objectFit: 'cover', borderRadius: 'var(--radius-sm)' }}
/>
```

### 2. Availability badge
```jsx
<span className={`badge ${book.copiesAvailable > 0 ? 'badge-green' : 'badge-red'}`}>
  {book.copiesAvailable > 0 ? `${book.copiesAvailable} available` : 'Unavailable'}
</span>
```

### 3. Borrow button — disabled if unavailable or not logged in
```jsx
<button
  className="btn btn-primary"
  disabled={book.copiesAvailable === 0 || !user}
  onClick={() => handleBorrow(book.id)}
>
  {!user ? 'Login to borrow' : book.copiesAvailable > 0 ? 'Borrow' : 'Unavailable'}
</button>
```

### 4. Status badge in borrow history
```jsx
<span className={`badge ${borrow.status === 'ACTIVE' ? 'badge-green' : 'badge-grey'}`}>
  {borrow.status}
</span>
```

### 5. Loading states on buttons
```jsx
const [loading, setLoading] = useState(false)

const handleBorrow = async (bookId) => {
  setLoading(true)
  try {
    await borrowApi.borrowBook(bookId)
  } catch (err) {
    setError(err.response?.data?.message || 'Something went wrong')
  } finally {
    setLoading(false)  // always runs, even on failure
  }
}

<button disabled={loading} onClick={() => handleBorrow(book.id)}>
  {loading ? 'Borrowing...' : 'Borrow Book'}
</button>
```

### 6. Empty state
```jsx
{items.length === 0 && (
  <div className="empty-state">
    <div className="icon">📭</div>
    <p>You haven't borrowed any books yet.</p>
    <button className="btn btn-primary" onClick={() => navigate('/books')}>
      Browse catalogue
    </button>
  </div>
)}
```

---

_Last updated: Day 15 ✅ — Design system locked. Auth pages complete. Next: bookApi.js → BookCard → BooksPage → ProtectedRoute._