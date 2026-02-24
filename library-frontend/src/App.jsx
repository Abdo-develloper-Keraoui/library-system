import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Navbar from './components/Navbar'
import GuestRoute from './components/GuestRoute'
import ProtectedRoute from './components/ProtectedRoute'
import AdminRoute from './components/AdminRoute'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import HomePage from './pages/HomePage'
import BooksPage from './pages/BooksPage'
import BookDetailPage from './pages/BookDetailPage'
import MyBorrowsPage from './pages/MyBorrowsPage'
import AdminBooksPage from './pages/admin/AdminBooksPage'
import BookFormPage from './pages/admin/BookFormPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Navbar />
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/books" element={<BooksPage />} />
          <Route path="/books/:id" element={<BookDetailPage />} />
          <Route path="/my-borrows" element={
            <ProtectedRoute><MyBorrowsPage /></ProtectedRoute>
          } />
          <Route path="/admin/books" element={
            <AdminRoute><AdminBooksPage /></AdminRoute>
          } />
          <Route path="/admin/books/new" element={
            <AdminRoute><BookFormPage /></AdminRoute>
          } />
          <Route path="/admin/books/edit/:id" element={
            <AdminRoute><BookFormPage /></AdminRoute>
          } />
          <Route path="/admin/users" element={
            <AdminRoute><AdminUsersPage /></AdminRoute>
          } />
          <Route path="/login" element={
            <GuestRoute><LoginPage /></GuestRoute>
          } />
          <Route path="/register" element={
            <GuestRoute><RegisterPage /></GuestRoute>
          } />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App