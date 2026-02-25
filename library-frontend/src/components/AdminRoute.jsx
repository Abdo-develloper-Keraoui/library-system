import { useAuth } from '../context/AuthContext'
import { Navigate } from 'react-router-dom'

function AdminRoute({ children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" />
  if (user.role !== 'ADMIN') return <Navigate to="/" />
  return children
}

export default AdminRoute