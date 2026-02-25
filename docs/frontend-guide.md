# Frontend Guide

How the frontend is structured, how the design system works, and how data flows between the UI and the backend.

---

## Folder Structure

```
library-frontend/src/
│
├── api/                            ← ALL HTTP calls live here — nowhere else
│   ├── axiosInstance.js            base URL + auto-attach JWT interceptor
│   ├── authApi.js                  register(), login()
│   ├── bookApi.js                  getAllBooks(), getBookById(), createBook(), updateBook(), deleteBook()
│   ├── borrowApi.js                borrowBook(), returnBook(), getMyBorrows(), getAllBorrows()
│   └── adminApi.js                 getAllUsers(), toggleSuspend(), deleteUser()
│
├── context/
│   └── AuthContext.jsx             stores token, email, role, firstName
│
├── components/
│   ├── Navbar.jsx                  Rivendell themed, role-based links, ꧁ name ꧂ pill
│   ├── BookCard.jsx                clickable card — cover image, title, author, genre badge, availability badge
│   ├── GuestRoute.jsx              redirects logged-in users away from /login and /register
│   ├── ProtectedRoute.jsx          redirects unauthenticated users to /login
│   └── AdminRoute.jsx              redirects non-admins: no user → /login, wrong role → /
│
├── pages/
│   ├── HomePage.jsx                hero section + animated scrolling book belt
│   ├── LoginPage.jsx               uses raw fetch (see Known Issues)
│   ├── RegisterPage.jsx            uses authApi.register(), auto-login on success
│   ├── BooksPage.jsx               book grid + client-side genre filter
│   ├── BookDetailPage.jsx          single book view + borrow button
│   ├── MyBorrowsPage.jsx           user borrow history with inline return button
│   └── admin/
│       ├── AdminBooksPage.jsx      book list with edit/delete (window.confirm for delete)
│       ├── AdminUsersPage.jsx      user list with suspend/unsuspend and inline delete confirm
│       └── BookFormPage.jsx        shared add/edit form, detects mode via useParams id
│
├── styles/
│   └── global.css                  Rivendell Reads design system
│
├── App.jsx                         BrowserRouter + AuthProvider + Navbar + all routes
└── main.jsx                        entry point, imports global.css
```

---

## Design System — Rivendell Reads

### Philosophy
Tolkien-inspired. Warm parchment backgrounds, dark brown navbar, jade green primary actions, muted gold accents. All color decisions live in CSS variables — no hardcoded hex values in component files.

### Fonts (loaded in `index.html` via Google Fonts)
- `Cinzel` — brand name and hero heading only
- `Merriweather` — page headings
- `Lato` — body text and buttons

### CSS Variables
```css
/* Backgrounds */
--bg-page:      #faf6ef;   /* warm parchment */
--bg-card:      #fffcf5;
--bg-section:   #f0e9db;   /* form inputs, section backgrounds */
--bg-navbar:    #2c1f14;   /* dark warm brown */

/* Accent Colors */
--jade:         #3d7a5c;   /* primary buttons, active states */
--jade-hover:   #2d5e45;
--brown:        #8b6340;
--gold:         #c49a3c;   /* logo, name pill, genre badges */
--danger:       #b85c3a;   /* delete/logout/return */
--danger-hover: #9a4a2c;

/* Text */
--text-heading: #1e1208;
--text-body:    #3d2b1a;
--text-medium:  #6b5032;
--text-light:   #9c866e;
--text-navbar:  #e8dcc8;

/* Borders */
--border:       #ddd0bb;
--border-focus: #3d7a5c;

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
`.btn`, `.btn-primary`, `.btn-danger`, `.btn-secondary`, `.btn-secondary-light`, `.card`, `.form-group`, `.badge`, `.badge-green`, `.badge-grey`, `.badge-red`, `.badge-gold`, `.error-msg`, `.success-msg`, `.empty-state`, `.page`, `.container`, `.divider`

**Secondary button variants:**
- `.btn-secondary` — transparent, `--text-navbar` color. Only for use inside the dark navbar
- `.btn-secondary-light` — `--bg-section` fill, `--text-body` color. Use everywhere else: back buttons, cancel actions, secondary actions on pages

---

## Route Guards

**`GuestRoute`** — wraps `/login` and `/register`. If `user` exists → `<Navigate to="/" />`. Prevents logged-in users from seeing auth pages.

**`ProtectedRoute`** — wraps `/my-borrows`. If no `user` → `<Navigate to="/login" />`.

**`AdminRoute`** — wraps all `/admin/*` routes. If no `user` → `/login`. If `user.role !== 'ADMIN'` → `/`. Handles both unauthenticated and unauthorized scenarios.

---

## AuthContext

`AuthContext` stores `user` as either `null` (not logged in) or:
```js
{ token, email, role, firstName }
```

Three operations:
- **On mount** — reads all four keys from `localStorage`. If `token` exists, restores `user`; otherwise `null`
- **`loginUser(data)`** — writes all four to `localStorage`, sets `user` state
- **`logoutUser()`** — removes all four from `localStorage`, sets `user` to `null`

**Known edge case:** On register, the backend returns `token: null` (a JWT is only generated on login). `RegisterPage` calls `loginUser(response.data)` which stores `null` as the token. The axiosInstance interceptor will attach `null` as the Bearer token on subsequent calls. A future fix would have the backend return a real JWT on registration, or have `RegisterPage` call `authApi.login()` immediately after registering.

---

## Pages

**`HomePage`** — Landing page at `/`. Two sections:
1. Hero: brand name in Cinzel gold, tagline, Login/Register buttons (or Browse Books if logged in)
2. Book belt: animated horizontal scroll of cover images. Array is doubled (`[...books, ...books]`) for a seamless loop. `@keyframes scrollBelt` runs 40s linear infinite. Hovering pauses. Only books with `coverImageUrl` render.

**`BooksPage`** — Book grid at `/books`. Genre filter is fully client-side: `new Set(books.map(b => b.genre).filter(Boolean))`. Active genre pill turns jade green.

**`BookDetailPage`** — Single book at `/books/:id`. Borrow button is disabled if `copiesAvailable === 0`, user is not logged in, user is admin, or `borrowLoading`. On borrow success, decrements `copiesAvailable` locally without refetching.

**`MyBorrowsPage`** — Borrow history at `/my-borrows` (`ProtectedRoute`). Active borrows show a Return button. On return, updates the borrow's status in local state to `'RETURNED'` without refetching.

**`AdminBooksPage`** — Book management at `/admin/books` (`AdminRoute`). Delete uses `window.confirm()`.

**`AdminUsersPage`** — User management at `/admin/users` (`AdminRoute`). Status badge and Suspend button label are driven by `user.active` (not `user.isActive` — see Known Issues). Delete uses a styled inline confirmation bar.

**`BookFormPage`** — Shared add/edit at `/admin/books/new` and `/admin/books/edit/:id`. Mode detected by `const isEditing = Boolean(id)`. Live cover image preview when `form.coverImageUrl` is non-empty.

**`LoginPage`** — Uses raw `fetch` directly (see Known Issues).

**`RegisterPage`** — Uses `authApi.register()`. Auto-login on success.

---

## adminApi.js

```js
getAllUsers()           → GET    /admin/users
toggleSuspend(userId)  → PUT    /admin/users/:userId/suspend
deleteUser(userId)     → DELETE /admin/users/:userId
```

All go through `axiosInstance` and automatically carry the JWT.

---

## Known Issues / Future Improvements

**`LoginPage` uses raw `fetch`** — `LoginPage` was written before `axiosInstance.js` was set up. All other pages use the centralized API layer. Future fix: refactor `LoginPage` to call `authApi.login()`.

**`user.active` vs `user.isActive`** — Java serializes `boolean isActive` as `active` in JSON (strips the `is` prefix). React must read `user.active`. This is handled correctly in the current codebase but worth noting for anyone extending `AdminUsersPage`.

**`AdminBooksPage` delete uses `window.confirm()`** — `AdminUsersPage` has the styled inline confirmation bar. `AdminBooksPage` still uses `window.confirm()`. Consistent treatment across both pages is a cleanup opportunity.

**Token on register is null** — see AuthContext section above.

---

## Request Flows

### User logs in
```
LoginPage → fetch POST /api/v1/auth/login {email, password}
         ← 200 {token, email, role, firstName}
         → loginUser(data) → AuthContext + localStorage
         → navigate('/')
```

### User registers
```
RegisterPage → authApi.register(email, password, firstName, lastName)
             → POST /api/v1/auth/register
             ← 201 {token: null, email, role, firstName}
             → loginUser(response.data) → AuthContext
             → navigate('/')
```

### Browse books (no auth)
```
BooksPage → getAllBooks() → GET /api/v1/books
          ← 200 [{id, title, author, genre, copiesAvailable, coverImageUrl...}]
          → genres computed client-side with Set
          → render BookCard for each filtered book
```

### User borrows a book
```
BookDetailPage → borrowBook(id) → POST /api/v1/borrows/:bookId/borrow
                 Authorization: Bearer <token>  ← axiosInstance interceptor
              ← 200 {id, bookTitle, dueDate, status: ACTIVE}
              → decrement copiesAvailable locally
```

### Admin suspends a user
```
AdminUsersPage → toggleSuspend(userId) → PUT /api/v1/admin/users/:id/suspend
              ← 200 {id, firstName, ..., isActive: false}
              → update local state: user.active = res.data.active
              → badge switches to Suspended
```

### Admin deletes a user
```
AdminUsersPage → setConfirmDeleteId(user.id)  ← opens inline confirm bar
              → handleDelete(user.id)
              → deleteUser(userId) → DELETE /api/v1/admin/users/:id
              ← 204
              → filter user out of local state
```
