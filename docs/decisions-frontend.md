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

**Why:** UI libraries are a crutch that hide how CSS actually works. Using plain CSS means we understand every style choice. It also keeps the bundle smaller and avoids learning a UI library's API on top of everything else. The design is minimal but intentional — white background, one accent color (`#2563eb`), Lato font.

**Shared classes available:** `.btn`, `.btn-primary`, `.btn-danger`, `.btn-secondary`, `.card`, `.form-group`, `.badge`, `.badge-green`, `.badge-grey`, `.badge-red`, `.error-msg`, `.empty-state`, `.page`, `.container`

**Ruled out:** Tailwind CSS, Material UI, Bootstrap, Chakra UI.

---

## 4. HTTP Client: Axios (not raw fetch)

**Decision:** Use Axios via a shared `axiosInstance.js` for all API calls.

**Why:** Axios allows us to configure a base URL and attach the JWT token automatically via an interceptor — meaning every authenticated request gets the `Authorization: Bearer <token>` header without repeating that logic in every file. Raw `fetch` would require manual token attachment everywhere.

**The interceptor pattern:**

```js
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

**Ruled out:** Raw `fetch` for all calls (still used in LoginPage initially — later refactored to Axios for consistency).

---

## 5. Auth State: React Context (not Redux)

**Decision:** Store authentication state (token, email, role) in a React Context called `AuthContext`.

**Why:** Redux is overkill for this scope. We have one piece of shared state — whether the user is logged in. Context handles this cleanly. `AuthProvider` wraps the whole app so any component can call `useAuth()` to get the current user or call `loginUser()`/`logoutUser()`.

**Ruled out:** Redux, Zustand, or other state management libraries.

---

## 6. Token Storage: localStorage

**Decision:** Store the JWT token in `localStorage`.

**Why:** Simple, persistent across page refreshes, and appropriate for a portfolio project at this scope. The token is read back on page load to restore the auth session.

**Known tradeoff:** `localStorage` is vulnerable to XSS attacks. In a production application, `httpOnly` cookies would be preferred. This is worth mentioning in the project README as a known limitation and future improvement.

**Ruled out:** In-memory only (token lost on refresh), cookies (more complex setup).

---

## 7. Routing: React Router DOM

**Decision:** Use React Router DOM v6 for client-side routing.

**Why:** The industry standard for React routing. v6 uses a declarative `<Routes>` / `<Route>` API that's clean and easy to follow.

**Route protection strategy:**

- **GuestRoute** — redirects logged-in users away from `/login` and `/register`
- **ProtectedRoute** — redirects unauthenticated users away from private pages
- **AdminRoute** — redirects non-admin users away from admin pages

**Ruled out:** No routing (single page), Next.js (full-stack framework, overkill here).

---

## 8. API Layer: Centralized api/ folder

**Decision:** All backend calls live in `src/api/` — one file per domain (`authApi.js`, `bookApi.js`, `borrowApi.js`).

**Why:** Pages should not contain raw HTTP calls. Separating API logic means if the backend URL or contract changes, we fix it in one place — not scattered across every page component. This mirrors the service layer pattern used in the Spring Boot backend.

**Ruled out:** Inline fetch/axios calls inside page components.

---

## 9. Bug Found & Fixed — `authApi.js` register function (Day 14)

**Bug:** The `register()` function in `authApi.js` was only accepting and sending `email` and `password`. The `firstName` and `lastName` parameters were declared in `RegisterPage.jsx` and passed to the function — but the function signature ignored them.

**Symptom:** Backend returned `400 — first name is required`. Network tab showed only `email` and `password` in the request body.

**Diagnosis method:** Checked the Network tab → Payload in browser DevTools. Saw the request body was missing fields. Traced the data flow from input field → state → API call → found the broken link in `authApi.js`.

**Fix:**

```js
// Before (broken)
export const register = (email, password) => {
  return axiosInstance.post('/auth/register', { email, password })
}

// After (fixed)
export const register = (email, password, firstName, lastName) => {
  return axiosInstance.post('/auth/register', { email, password, firstName, lastName })
}
```

**Lesson:** The error appeared in the Network tab, but the bug was one layer deeper in the API layer. Always trace the full data flow before assuming where the bug is.

---

## 10. UX Decision — Auto-login after registration

**Decision:** After a successful registration, immediately call `loginUser(response.data)` and redirect to `/` — the user is logged in automatically.

**Why:** The backend returns a JWT token on registration, the same as login. There's no reason to make the user log in again right after creating their account. This matches the UX pattern of modern apps (GitHub, Notion, etc.).

---

_Last updated: Day 14_

