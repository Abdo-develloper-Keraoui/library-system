# Frontend Decisions Log

A running log of technical decisions made during the Library Management System frontend build. Each entry explains what we chose, why we chose it, and what we ruled out.

---

## 1. Framework: React + Vite (not Create React App)

**Decision:** Use Vite as the build tool instead of Create React App (CRA).

**Why:** CRA is largely abandoned and slow. Vite is the modern standard — it starts up almost instantly, reloads changes in milliseconds, and is what the industry uses in 2024+. For a portfolio project, being on Vite signals awareness of current tooling.

**Ruled out:** Create React App.

---

## 2. Language: JavaScript (not TypeScript)

**Decision:** Write all frontend code in plain JavaScript (.js / .jsx), not TypeScript.

**Why:** The student has zero prior JavaScript experience. Adding TypeScript on top would introduce a second learning curve at the same time. The backend already enforces strong types and validation — the frontend doesn't need to duplicate that for a portfolio project at this scope.

**Ruled out:** TypeScript. Can be added later as a learning exercise once the project is complete.

---

## 3. Styling: Plain CSS (no UI libraries)

**Decision:** All styles live in a single `src/styles/global.css` file using plain CSS classes. No Tailwind, no Material UI, no Bootstrap.

**Why:** UI libraries are a crutch that hide how CSS actually works. Using plain CSS means we understand every style choice. It also keeps the bundle smaller and avoids learning a UI library's API on top of everything else.

**Ruled out:** Tailwind CSS, Material UI, Bootstrap, Chakra UI.

---

## 4. HTTP Client: Axios (not raw fetch)

**Decision:** Use Axios via a shared `axiosInstance.js` for all API calls.

**Why:** Axios allows us to configure a base URL and attach the JWT token automatically via an interceptor — meaning every authenticated request gets the `Authorization: Bearer <token>` header without repeating that logic in every file.

**The interceptor pattern:**
```js
axiosInstance.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})
```

**Ruled out:** Raw `fetch` for all calls.

---

## 5. Auth State: React Context (not Redux)

**Decision:** Store authentication state (token, email, role, firstName) in a React Context called `AuthContext`.

**Why:** Redux is overkill for this scope. Context handles this cleanly. `AuthProvider` wraps the whole app so any component can call `useAuth()`.

**Ruled out:** Redux, Zustand, or other state management libraries.

---

## 6. Token Storage: localStorage

**Decision:** Store the JWT token and user info (email, role, firstName) in `localStorage`.

**Why:** Simple, persistent across page refreshes, appropriate for a portfolio project. The token is read back on page load to restore the auth session.

**Known tradeoff:** `localStorage` is vulnerable to XSS attacks. In production, `httpOnly` cookies would be preferred. Worth mentioning in the README as a known limitation.

**Ruled out:** In-memory only (lost on refresh), cookies (more complex setup).

---

## 7. Routing: React Router DOM

**Decision:** Use React Router DOM v6 for client-side routing.

**Route protection strategy:**
- **GuestRoute** — redirects logged-in users away from `/login` and `/register`
- **ProtectedRoute** — redirects unauthenticated users away from private pages
- **Role check in Navbar** — admin links only visible to ADMIN role

**Ruled out:** No routing (single page), Next.js (full-stack framework, overkill here).

---

## 8. API Layer: Centralized api/ folder

**Decision:** All backend calls live in `src/api/` — one file per domain (`authApi.js`, `bookApi.js`, `borrowApi.js`).

**Why:** Pages should not contain raw HTTP calls. Separating API logic means if the backend URL or contract changes, we fix it in one place. This mirrors the service layer pattern used in the Spring Boot backend.

**Ruled out:** Inline fetch/axios calls inside page components.

---

## 9. Bug Found & Fixed — `authApi.js` register function (Day 14)

**Bug:** The `register()` function was only sending `email` and `password`. `firstName` and `lastName` were ignored.

**Fix:**
```js
export const register = (email, password, firstName, lastName) => {
    return axiosInstance.post('/auth/register', { email, password, firstName, lastName })
}
```

**Lesson:** Always trace the full data flow before assuming where the bug is. Network tab → Payload revealed the missing fields.

---

## 10. UX Decision — Auto-login after registration (Day 14)

**Decision:** After successful registration, immediately call `loginUser(response.data)` and redirect to `/`.

**Why:** The backend returns a JWT token on registration. No reason to make the user log in again immediately after creating their account.

---

## 11. firstName added to AuthContext (Day 15)

**Decision:** Store `firstName` in AuthContext alongside token, email, and role.

**Why:** The Navbar needs to display the user's first name on every page. Storing it in AuthContext means any component can access it via `useAuth()` without extra API calls.

**Three places updated in AuthContext:**
1. Reading from localStorage on startup — `localStorage.getItem('firstName')`
2. `loginUser()` — save to localStorage + state
3. `logoutUser()` — remove from localStorage + state

---

## 12. Design System: Rivendell Reads theme (Day 15)

**Decision:** Tolkien-inspired warm theme. Parchment backgrounds, jade green accent, muted gold highlights, dark brown navbar.

**Why:** Distinctive and memorable for a portfolio project. Stands out from generic blue-on-white apps. Shows design intention without complexity.

**Colors (CSS variables):**
- `--bg-page: #faf6ef` — warm parchment
- `--bg-navbar: #2c1f14` — dark warm brown
- `--jade: #3d7a5c` — primary buttons
- `--gold: #c49a3c` — logo, name pill, accents
- `--danger: #b85c3a` — terracotta for delete/logout

**Fonts:**
- `Cinzel` — brand name only (elvish-feeling)
- `Merriweather` — page headings (warm serif)
- `Lato` — body text and buttons (clean)

**Rule:** All colors must use CSS variables. No hardcoded hex values in component files.

---

## 13. Logo treatment: text only, no symbol (Day 15)

**Decision:** Logo is just `Rivendell Reads` in Cinzel gold. No emoji or symbol prefix.

**Why:** The Cinzel font already looks distinctive and elvish on its own. Adding a symbol felt "AI-generated." Clean restraint looks more professional.

**Ruled out:** ✦ prefix, emoji, image file.

---

## 14. Navbar name display: ornamental brackets (Day 15)

**Decision:** Display logged-in user's name as `꧁ firstName ꧂` in Cinzel gold font.

**Why:** The `꧁` `꧂` characters are ornamental Unicode brackets that feel elvish and intentional. More distinctive than a plain name or generic "Hello, X" greeting.

---

## 15. Delete confirmation on AdminUsersPage: inline confirm bar — not `window.confirm()` (Day 17)

**Decision:** Replace `window.confirm()` on AdminUsersPage with a styled inline confirmation bar that renders inside the user's card.

**How it works:** `confirmDeleteId` state tracks which user's confirmation is currently open. Clicking Delete sets `confirmDeleteId = user.id`. A bar appears inside that card with "Delete **FirstName**? This cannot be undone." and Cancel / "Yes, delete" buttons. Cancel clears the state. Confirm calls the real delete handler.

**Why:** `window.confirm()` is a browser-native modal that is unstyled, cannot use CSS variables, and blocks JavaScript execution. The inline bar stays within the Rivendell Reads design system, requires zero external libraries, and is implemented with one piece of React state.

**Note:** `AdminBooksPage` still uses `window.confirm()` for book deletion — the styled inline confirm was only applied to users. Consistent treatment across both pages is a potential Day 20 cleanup.

**Ruled out:** `window.confirm()`, third-party modal libraries.

---

## 16. AdminRoute: separate component for role-based route protection (Day 17)

**Decision:** Created a dedicated `AdminRoute.jsx` component alongside the existing `ProtectedRoute` and `GuestRoute`.

**How it works:**
```jsx
function AdminRoute({ children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" />
  if (user.role !== 'ADMIN') return <Navigate to="/" />
  return children
}
```

**Why:** `ProtectedRoute` only checks if a user is logged in — it doesn't check role. Wrapping admin routes with `AdminRoute` handles both scenarios: unauthenticated users go to login, authenticated non-admins go to home. Three distinct guard components, each with a single clear responsibility.

**Ruled out:** Putting role checks inside each admin page component (duplication), a single generic route guard with a `role` prop (less explicit).

---

## 17. adminApi.js: separate file for admin HTTP calls (Day 17)

**Decision:** Admin user management API calls live in their own `src/api/adminApi.js` file.

**Why:** Mirrors the separation pattern already used for `authApi`, `bookApi`, `borrowApi`. Admin calls are a distinct domain — keeping them separate makes the api/ folder easy to scan. Any developer can immediately see where admin-specific backend calls are made.

---

## 18. HomePage: landing page with animated book belt (Day 17)

**Decision:** Route `/` renders a dedicated `HomePage` (not `BooksPage`). It has two sections: a hero with a call-to-action, and an animated horizontal scroll of book covers.

**Belt implementation:** All books are fetched from the API. The array is doubled (`[...books, ...books]`) to make the scroll seamless — when the first copy scrolls out, the second copy is already in place. Only books with `coverImageUrl` render in the belt. CSS `@keyframes scrollBelt` handles the animation (40s linear infinite). Hovering pauses playback.

**Why:** A static page with just Login/Register buttons is boring. The book belt makes the app feel alive immediately and shows real catalogue data without requiring a login. It costs one `getAllBooks()` call that would be made anyway when the user navigates to BooksPage.

**Ruled out:** Making BooksPage the landing page, a static hero with no dynamic content.

---

## 19. LoginPage uses raw fetch — known inconsistency (Day 17)

**Decision:** `LoginPage` was the first page built and uses the browser's native `fetch` API directly rather than `axiosInstance`.

**Why it happened:** `LoginPage` was written before `axiosInstance.js` was set up. All subsequent pages use `axiosInstance` via the api/ files.

**Current state:** `LoginPage` calls `fetch('http://localhost:8081/api/v1/auth/login', ...)` directly and handles the response manually. This is the only page that bypasses the centralized API layer.

**Future fix:** Refactor `LoginPage` to call `authApi.login(email, password)` to match the rest of the codebase. Low priority — it works correctly, it's just inconsistent.

---

_Last updated: Day 17 ✅_