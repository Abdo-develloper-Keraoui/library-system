import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('token')
    const email = localStorage.getItem('email')
    const role = localStorage.getItem('role')
    const firstName = localStorage.getItem('firstName')
    return token ? { token, email, role, firstName } : null
  })

  const loginUser = (data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('email', data.email)
    localStorage.setItem('role', data.role)
    localStorage.setItem('firstName', data.firstName)
    setUser({ token: data.token, email: data.email, role: data.role, firstName: data.firstName})
  }

  const logoutUser = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('email')
    localStorage.removeItem('role')
    localStorage.removeItem('firstName')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loginUser, logoutUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)