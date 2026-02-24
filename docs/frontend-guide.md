> Frontend started Day 13. Complete as of Day 17. All pages and routes implemented.

---

## Folder Structure

```
library-frontend/src/
│
├── api/                            ← ALL HTTP calls live here — nowhere else
│   ├── axiosInstance.js            ✅ base URL (localhost:8081/api/v1) + auto-attach JWT
│   ├── authApi.js                  ✅ register(), login()
│   ├── bookApi.js                  ✅ getAllBooks(), getBookById(), createBook(), updateBook(), deleteBook()
│   ├── borrowApi.js                ✅ borrowBook(), returnBook(), getMyBorrows(), getAllBorrows()
│   └── adminApi.js                 ✅ getAllUsers(), toggleSuspend(), deleteUser()
│
├── context/
│   └── AuthContext.jsx             ✅ stores token, email, role, firstName
│
├── components/
│   ├── Navbar.jsx                  ✅ Rivendell themed, role-based links, ꧁ name ꧂ pill
│   ├── BookCard.jsx                ✅ clickable card — cover image, title, author, genre badge, availability badge
│   ├── GuestRoute.jsx              ✅ redirects logged-in users away from /login and /register
│   ├── ProtectedRoute.jsx          ✅ redirects unauthenticated users to /login
│   └── AdminRoute.jsx              ✅ redirects non-admins: no user → /login, wrong role → /
│
├── pages/
│   ├── HomePage.jsx                ✅ hero section + animated scrolling book belt
│   ├── LoginPage.jsx               ✅ uses raw fetch (see note below)
│   ├── RegisterPage.jsx            ✅ uses authApi.register(), auto-login on success
│   ├── BooksPage.jsx               ✅ book grid + client-side genre filter
│   ├── BookDetailPage.jsx          ✅ single book view + borrow button
│   ├── MyBorrowsPage.jsx           ✅ user borrow history with inline return button
│   └── admin/
│       ├── AdminBooksPage.jsx      ✅ book list with edit/delete (uses window.confirm for delete)
│       ├── AdminUsersPage.jsx      ✅ user list with suspend/unsuspend and styled inline delete confirm
│       └── BookFormPage.jsx        ✅ shared add/edit form, detects mode via useParams id
│
├── styles/
│   └── global.css                  ✅ Rivendell Reads design system (see below)
│
├── App.jsx                         ✅ BrowserRouter + AuthProvider + Navbar + all routes
└── main.jsx                        ✅ entry point, imports global.css
```

---

## Design System — Rivendell Reads

### Philosophy
Tolkien-inspired. Warm parchment backgrounds, dark brown navbar, jade green accents, muted gold highlights. Distinctive and memorable for a portfolio project.

### Fonts (loaded in index.html via Google Fonts)
```html
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@400;600;700&family=Merriweather:wght@400;700&family=Lato:wght@400;600&display=swap" rel="stylesheet">
```
- `Cinzel` → brand name and hero heading only
- `Merriweather` → page headings (h1 default, h2, h3 in global.css)
- `Lato` → body text and buttons

### CSS Variables (actual values from global.css)
```css
/* Backgrounds */
--bg-page:      #faf6ef;   /* warm parchment */
--bg-card:      #fffcf5;   /* slightly lighter for cards */
--bg-section:   #f0e9db;   /* darker parchment — section backgrounds, form inputs */
--bg-navbar:    #2c1f14;   /* dark warm brown */

/* Accent Colors */
--jade:         #3d7a5c;   /* primary buttons, active states */
--jade-hover:   #2d5e45;
--brown:        #8b6340;   /* warm brown — secondary elements */
--gold:         #c49a3c;   /* logo, name pill, genre badges */
--danger:       #b85c3a;   /* delete/logout/return */
--danger-hover: #9a4a2c;

/* Text */
--text-heading: #1e1208;   /* very dark warm brown */
--text-body:    #3d2b1a;   /* warm dark brown */
--text-medium:  #6b5032;   /* secondary text */
--text-light:   #9c866e;   /* hints, placeholders */
--text-navbar:  #e8dcc8;   /* warm cream on dark navbar */

/* Borders */
--border:       #ddd0bb;
--border-focus: #3d7a5c;   /* jade on focus */

/* Shadows */
--shadow-sm:    0 1px 3px rgba(44,31,20,0.08), 0 2px 8px rgba(44,31,20,0.04);
--shadow-md:    0 4px 12px rgba(44,31,20,0.10), 0 8px 24px rgba(44,31,20,0.06);

/* Typography */
--font-brand:   'Cinzel', 'Georgia', serif;
--font-heading: 'Merriweather', 'Georgia', serif;
--font-body:    'Lato', 'Helvetica', sans-serif;

/* Spacing (multiples of 8) */
--space-xs: 8px;  --space-sm: 16px;  --space-md: 24px;
--space-lg: 32px; --space-xl: 48px;  --space-2xl: 64px;

/* Radius */
--radius-sm: 6px;  --radius-md: 10px;  --radius-lg: 16px;
```

### Available CSS Classes
`.btn`, `.btn-primary`, `.btn-danger`, `.btn-secondary` (navbar only), `.btn-secondary-light` (on light backgrounds), `.card`, `.form-group`, `.badge`, `.badge-green`, `.badge-grey`, `.badge-red`, `.badge-gold`, `.error-msg`, `.success-msg`, `.empty-state`, `.page`, `.container`, `.divider`

**The two secondary button variants:**
- `.btn-secondary` — transparent with `--text-navbar` color and semi-transparent border. Only for use inside the dark navbar.
- `.btn-secondary-light` — `--bg-section` fill, `--text-body` color, `--border` border. Use everywhere else: back buttons, cancel buttons, secondary actions on pages.

### Golden Rule
**Never hardcode hex colors in component files. Always use CSS variables.**

---

## Route Guards — Three Types

**`GuestRoute`** — wraps `/login` and `/register`. If `user` exists in context → `<Navigate to="/" />`. Prevents logged-in users from seeing auth pages.

**`ProtectedRoute`** — wraps `/my-borrows`. If no `user` → `<Navigate to="/login" />`. Prevents unauthenticated access.

**`AdminRoute`** — wraps all `/admin/*` routes. Two checks: if no `user` → `<Navigate to="/login" />`. If `user.role !== 'ADMIN'` → `<Navigate to="/" />`. Prevents non-admins from reaching admin pages even with a valid token.

---

## Key Design Decisions

**Why `axiosInstance.js`?** Base URL (`http://localhost:8081/api/v1`) and JWT header set once. Every API call automatically gets the right headers via the request interceptor.

**Why `AuthContext`?** JWT token + user info needs to be accessible everywhere — Navbar, route guards, all pages. Context is the simplest way to share state globally without prop drilling.

**Why three route guards?** `GuestRoute` and `ProtectedRoute` handle authentication state. `AdminRoute` adds role checking — it's a separate component so any route can be wrapped independently.

**Why `BookFormPage` handles both add and edit?** The form is identical. Mode is detected by `const isEditing = Boolean(id)` from `useParams()`. Add route: `/admin/books/new`. Edit route: `/admin/books/edit/:id`. One component, two routes, no duplication.

**LoginPage uses raw `fetch` instead of `axiosInstance`** — `LoginPage` was written before `axiosInstance` was set up and uses the browser's native `fetch` directly. All other pages use `axiosInstance`. This is a minor inconsistency worth noting — `LoginPage` could be refactored to use `authApi.login()` to match the rest of the codebase.

---

## AuthContext — What's Stored

`AuthContext` stores `user` as either `null` (not logged in) or an object:
```js
{ token, email, role, firstName }
```

Three operations:
- **On mount** (lazy `useState` initializer) — reads all four keys from `localStorage`. If `token` exists, restores `user`; otherwise `null`.
- **`loginUser(data)`** — writes all four to `localStorage`, sets `user` state.
- **`logoutUser()`** — removes all four from `localStorage`, sets `user` to `null`.

Note: On `register`, the backend returns `token: null` (no JWT is generated on register). `RegisterPage` calls `loginUser(response.data)` which stores `null` as the token. This means a freshly registered user has `user` set in context but `token` is null in localStorage — the axiosInstance interceptor will attach `null` as the Bearer token on subsequent calls. This is a known inconsistency — a future fix would have the register endpoint return a real JWT, or have `RegisterPage` call `login` immediately after register.

---

## Pages — What Each Does

**`HomePage`** — Landing page at `/`. Two sections:
1. Hero: brand name in Cinzel gold, Frank Zappa quote, Login/Register buttons (or Browse Books if logged in)
2. Book belt: animated horizontal scroll of cover images fetched from `getAllBooks()`. The array is doubled (`[...books, ...books]`) to create a seamless loop. CSS `@keyframes scrollBelt` runs 40s linear infinite. Hovering pauses the animation. Only books with `coverImageUrl` render in the belt.

**`BooksPage`** — Book grid at `/books`. Fetches all books on mount. Genre filter is fully client-side: `new Set(books.map(b => b.genre).filter(Boolean))` — deduplicates genres, drops nulls. Pill buttons above the grid. Active genre button turns jade green. Renders `BookCard` for each filtered book.

**`BookDetailPage`** — Single book at `/books/:id`. Fetches by ID. Borrow button is disabled if `copiesAvailable === 0`, if user is not logged in, or if `borrowLoading`. On borrow success, decrements `copiesAvailable` locally without refetching.

**`MyBorrowsPage`** — Borrow history at `/my-borrows` (ProtectedRoute). Each card shows title, borrow date, due date, status badge. Active borrows show a Return button. On return, updates the borrow's status in local state to `'RETURNED'` without refetching.

**`AdminBooksPage`** — Book management at `/admin/books` (AdminRoute). Table/list of all books with Edit and Delete buttons. Delete uses `window.confirm()` — a simple native dialog (unlike AdminUsersPage which has the styled inline confirm).

**`AdminUsersPage`** — User management at `/admin/users` (AdminRoute). Each user card shows name, email, role badge (`badge-gold`), and status badge (`badge-green` / `badge-red` based on `user.active`). Suspend/Unsuspend button label changes based on `user.active`. Delete uses a styled inline confirmation bar inside the card — `confirmDeleteId` state tracks which card is open.

**`BookFormPage`** — Shared add/edit form at `/admin/books/new` and `/admin/books/edit/:id` (AdminRoute). Fields: title, author, isbn, pubYear, copiesAvailable (both required), genre, coverImageUrl (both optional). Live cover image preview appears when `form.coverImageUrl` is non-empty. `pubYear` and `copiesAvailable` are `parseInt`-ed before sending to the API.

**`LoginPage`** — Login form. Uses raw `fetch` (not axiosInstance). On success: `loginUser(data)` → `navigate('/')`.

**`RegisterPage`** — Register form. Uses `authApi.register()` (axiosInstance). On success: `loginUser(response.data)` → `navigate('/')`. Auto-login after registration.

---

## `adminApi.js` — Admin HTTP Calls

```js
getAllUsers()           → GET  /admin/users
toggleSuspend(userId)  → PUT  /admin/users/:userId/suspend
deleteUser(userId)     → DELETE /admin/users/:userId
```

All three go through `axiosInstance` and automatically carry the JWT. All three call the same backend `AdminUserController` endpoints.

Note: `AdminUsersPage` does NOT use `adminApi.js` — it imports and calls `getAllUsers`, `toggleSuspend`, `deleteUser` from `adminApi` via named imports. *(Verify this in AdminUsersPage if any changes are made.)*

---

## Request Flows

### Flow 1 — User logs in
```
LoginPage → fetch POST /api/v1/auth/login {email, password}
         ← 200 {token, email, role, firstName}
         → loginUser(data) → AuthContext + localStorage
         → navigate('/')
```

### Flow 2 — User registers
```
RegisterPage → authApi.register(email, password, firstName, lastName)
             → POST /api/v1/auth/register {firstName, lastName, email, password}
             ← 201 {token: null, email, role, firstName}
             → loginUser(response.data) → AuthContext (token is null)
             → navigate('/')
```

### Flow 3 — Browse books (no login needed)
```
BooksPage → getAllBooks() → GET /api/v1/books
          ← 200 [{id, title, author, genre, copiesAvailable, coverImageUrl...}]
          → genres computed client-side with Set
          → filteredBooks filtered client-side
          → render BookCard for each
```

### Flow 4 — User borrows a book
```
BookDetailPage → borrowBook(id) → POST /api/v1/borrows/:bookId/borrow
                 Authorization: Bearer <token>  ← axiosInstance adds this
              ← 200 {id, bookTitle, dueDate, status: ACTIVE}
              → setBorrowSuccess(...), decrement copiesAvailable locally
```

### Flow 5 — Admin suspends a user
```
AdminUsersPage → toggleSuspend(userId) → PUT /api/v1/admin/users/:id/suspend
              ← 200 {id, firstName, ..., isActive: false}
              → update local state: user.active = res.data.active
              → badge switches to Suspended (red)
```

### Flow 6 — Admin deletes a user
```
AdminUsersPage → setConfirmDeleteId(user.id)  ← opens inline confirm bar
              → handleDelete(user.id)
              → deleteUser(userId) → DELETE /api/v1/admin/users/:id
              ← 204
              → filter user out of local state
```

---

## AdminUsersPage — `user.active` vs `user.isActive`

Java serializes `boolean isActive` as `active` in JSON (strips the `is` prefix by convention). React must read `user.active`, NOT `user.isActive`.

`user.isActive` → always `undefined` → always falsy → every user looks suspended.
`user.active` → correct boolean from the API.

This applies everywhere in `AdminUsersPage`: the badge condition, the button label, and the local state update after a suspend toggle:
```js
setUsers(prev =>
  prev.map(u => u.id === userId ? { ...u, active: res.data.active } : u)
)
```

---

_Last updated: Day 17 ✅ — All pages complete. Next: Docker Compose (Day 18)._