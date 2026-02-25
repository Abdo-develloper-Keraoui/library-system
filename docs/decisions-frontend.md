# Architectural Decisions — Frontend

A log of technical decisions made during the frontend build. Each entry documents what was chosen, why, and what was ruled out.

---

## 1. Framework: React + Vite

**Decision:** Vite as the build tool instead of Create React App (CRA).

**Why:** CRA is largely abandoned and slow. Vite is the modern standard — near-instant startup, millisecond HMR, and the current industry default for React projects.

**Ruled out:** Create React App.

---

## 2. Language: JavaScript (not TypeScript)

**Decision:** All frontend code in plain JavaScript (`.js` / `.jsx`).

**Why:** The backend already enforces strong types and validation — the frontend does not need to duplicate that for this scope. TypeScript can be added as a future enhancement once the project is complete.

**Ruled out:** TypeScript.

---

## 3. Styling: Plain CSS — no UI libraries

**Decision:** All styles live in a single `src/styles/global.css` file using plain CSS classes.

**Why:** UI libraries abstract away how CSS actually works. Plain CSS keeps the bundle smaller, keeps all style decisions explicit and intentional, and avoids learning a UI library's API on top of React.

**Ruled out:** Tailwind CSS, Material UI, Bootstrap, Chakra UI.

---

## 4. HTTP Client: Axios with a shared instance

**Decision:** All API calls go through a shared `axiosInstance.js` configured with a base URL and a request interceptor.

**Why:** Axios allows the JWT token to be attached automatically on every authenticated request via an interceptor — no repetition across every API file.

```js
axiosInstance.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})
```

**Ruled out:** Raw `fetch` in every file.

---

## 5. Auth State: React Context

**Decision:** Authentication state (`token`, `email`, `role`, `firstName`) is stored in a React Context called `AuthContext`.

**Why:** Redux is overkill for this scope. `AuthProvider` wraps the entire app so any component can call `useAuth()` without prop drilling.

**Ruled out:** Redux, Zustand.

---

## 6. Token Storage: localStorage

**Decision:** The JWT token and user info are stored in `localStorage`, persisting across page refreshes.

**Known tradeoff:** `localStorage` is vulnerable to XSS attacks. In a higher-security production environment, `httpOnly` cookies would be preferred.

**Ruled out:** In-memory only (lost on refresh), cookies (more complex setup at this scope).

---

## 7. Routing: React Router DOM v6

**Decision:** React Router DOM v6 for client-side routing with three distinct route guard components.

**Route protection strategy:**
- `GuestRoute` — redirects logged-in users away from `/login` and `/register`
- `ProtectedRoute` — redirects unauthenticated users away from private pages
- `AdminRoute` — two checks: no user → `/login`, wrong role → `/`

**Ruled out:** No routing, Next.js.

---

## 8. API Layer: Centralized `src/api/` folder

**Decision:** All backend calls live in `src/api/` — one file per domain (`authApi.js`, `bookApi.js`, `borrowApi.js`, `adminApi.js`).

**Why:** Pages should not contain raw HTTP calls. Separating API logic means if the backend contract changes, it is fixed in one place. This mirrors the service layer pattern used in the backend.

**Ruled out:** Inline fetch/axios calls inside page components.

---

## 9. Auto-login after registration

**Decision:** After successful registration, `loginUser(response.data)` is called immediately and the user is redirected to `/`.

**Why:** The backend returns a JWT token on registration. Making the user log in again immediately after creating their account is unnecessary friction.

---

## 10. firstName stored in AuthContext

**Decision:** `firstName` is stored in `AuthContext` alongside `token`, `email`, and `role`.

**Why:** The Navbar displays the user's first name on every page. Storing it in context means any component can access it via `useAuth()` without extra API calls.

Three places updated in `AuthContext`:
1. Reading from `localStorage` on startup
2. `loginUser()` — save to `localStorage` + state
3. `logoutUser()` — remove from `localStorage` + state

---

## 11. Design System: Rivendell Reads theme

**Decision:** Tolkien-inspired warm theme. Parchment backgrounds, jade green accent, muted gold highlights, dark brown navbar.

**Colors (CSS variables):**
- `--bg-page: #faf6ef` — warm parchment
- `--bg-navbar: #2c1f14` — dark warm brown
- `--jade: #3d7a5c` — primary buttons
- `--gold: #c49a3c` — logo, name pill, accents
- `--danger: #b85c3a` — terracotta for delete/logout

**Fonts:**
- `Cinzel` — brand name only
- `Merriweather` — page headings
- `Lato` — body text and buttons

**Rule:** All colors use CSS variables. No hardcoded hex values in component files.

---

## 12. Logo treatment: text only, no symbol

**Decision:** Logo is `Rivendell Reads` in Cinzel gold. No emoji or symbol.

**Why:** The Cinzel font is distinctive on its own. Adding a symbol felt unnecessary.

---

## 13. Navbar name display: ornamental brackets

**Decision:** Display logged-in user's name as `꧁ firstName ꧂` in Cinzel gold.

**Why:** The Unicode ornamental brackets `꧁` `꧂` feel intentional and distinctive, consistent with the theme.

---

## 14. Delete confirmation: inline confirm bar, not `window.confirm()`

**Decision:** Replace `window.confirm()` with a styled inline confirmation bar that renders inside the card.

**How it works:** `confirmDeleteId` state tracks which card's confirmation is open. Clicking Delete sets `confirmDeleteId = id`. A bar appears inside that card with Cancel and Confirm buttons. Cancel clears the state.

**Why:** `window.confirm()` is unstyled and cannot use CSS variables. The inline bar stays within the design system with zero external libraries.

**Ruled out:** `window.confirm()`, third-party modal libraries.

---

## 15. AdminRoute: dedicated component for role-based route protection

**Decision:** A dedicated `AdminRoute.jsx` component, separate from `ProtectedRoute` and `GuestRoute`.

```jsx
function AdminRoute({ children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" />
  if (user.role !== 'ADMIN') return <Navigate to="/" />
  return children
}
```

**Why:** Three distinct guard components, each with a single clear responsibility. Role checks stay out of individual page components.

**Ruled out:** Role checks inside each admin page component, a single generic guard with a `role` prop.

---

## 16. AdminUsersPage: `user.active` not `user.isActive`

**Decision:** The React code reads `user.active` from the API response, not `user.isActive`.

**Why:** Java serializes `boolean isActive` as `active` in JSON — the Jackson serializer strips the `is` prefix by convention. Using `user.isActive` in React returns `undefined` (always falsy), making every user appear suspended.

This applies everywhere: the badge condition, the button label, and the local state update after a suspend toggle.

---

## 17. BookFormPage handles both add and edit

**Decision:** A single `BookFormPage.jsx` handles both creating and editing a book.

**How:** Mode is detected by `const isEditing = Boolean(id)` from `useParams()`.
- Add route: `/admin/books/new`
- Edit route: `/admin/books/edit/:id`

**Why:** The form is identical in both cases. One component, two routes, no duplication.

---

## 18. HomePage: animated book belt

**Decision:** Route `/` renders a dedicated `HomePage` with a hero section and an animated horizontal book belt.

**Belt implementation:** All books are fetched from the API. The array is doubled (`[...books, ...books]`) to make the scroll seamless. Only books with `coverImageUrl` render. CSS `@keyframes scrollBelt` runs 40s linear infinite. Hovering pauses the animation.

**Why:** A static landing page with login/register buttons alone is not engaging. The book belt makes the app feel alive and shows real catalogue data without requiring authentication.

---

## 19. LoginPage uses raw fetch — known inconsistency

**Current state:** `LoginPage` calls the login endpoint with the native `fetch` API directly rather than `axiosInstance`. This is the only page that bypasses the centralized API layer — it was written before `axiosInstance.js` was set up.

**Future fix:** Refactor `LoginPage` to call `authApi.login(email, password)`. Low priority — it functions correctly, it is just inconsistent with the rest of the codebase.
