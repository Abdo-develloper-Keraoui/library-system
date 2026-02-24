import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/authApi'
import { useAuth } from '../context/AuthContext'

function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const { loginUser } = useAuth()
  const navigate = useNavigate()

  const handleLogin = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await login(email, password)
      loginUser(response.data)
      navigate('/')
    } catch (err) {
      const message = err.response?.data?.message || 'Login failed'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-section)' }}>
      <div className="card" style={{ width: '100%', maxWidth: '400px' }}>
        <h2 style={{ marginBottom: '24px' }}>Login</h2>

        {error && <div className="error-msg">{error}</div>}

        <div className="form-group">
          <label>Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
          />
        </div>

        <div className="form-group">
          <label>Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
          />
        </div>

        <button
          className="btn btn-primary"
          style={{ width: '100%' }}
          onClick={handleLogin}
          disabled={loading}
        >
          {loading ? 'Logging in...' : 'Login'}
        </button>

        <p style={{ textAlign: 'center', marginTop: '16px', fontSize: '14px', color: 'var(--text-light)' }}>
          Don't have an account?{' '}
          <span
            style={{ color: 'var(--jade)', cursor: 'pointer', fontWeight: '600' }}
            onClick={() => navigate('/register')}
          >
            Register
          </span>
        </p>
      </div>
    </div>
  )
}

export default LoginPage