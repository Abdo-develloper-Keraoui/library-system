import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'

function Navbar() {
  const { user, logoutUser } = useAuth()
  const navigate = useNavigate()

  return (
    <nav style={{
      backgroundColor: 'var(--bg-navbar)',
      borderBottom: '2px solid var(--gold)',
      padding: '0 var(--space-md)',
      height: '64px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      position: 'sticky',
      top: 0,
      zIndex: 100,
    }}>

      {/* LEFT — Brand Logo */}
      <span
        onClick={() => navigate('/')}
        style={{
          fontFamily: 'var(--font-brand)',
          fontWeight: 700,
          fontSize: '1.2rem',
          cursor: 'pointer',
          color: 'var(--gold)',
          letterSpacing: '0.05em',
        }}
      >
         Rivendell Reads
      </span>

      {/* RIGHT — Navigation links */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-sm)' }}>

        {/* NOT logged in */}
        {!user && (
          <>
            <button className="btn btn-secondary" onClick={() => navigate('/login')}>Login</button>
            <button className="btn btn-primary" onClick={() => navigate('/register')}>Register</button>
          </>
        )}

        {/* Logged in — any role */}
        {user && (
          <>
            <button className="btn btn-secondary" onClick={() => navigate('/books')}>Browse Books</button>

            {/* USER only */}
            {user.role === 'USER' && (
              <button className="btn btn-secondary" onClick={() => navigate('/my-borrows')}>My Borrows</button>
            )}

            {/* ADMIN only */}
            {user.role === 'ADMIN' && (
              <>
                <button className="btn btn-secondary" onClick={() => navigate('/admin/books')}>Manage Books</button>
                <button className="btn btn-secondary" onClick={() => navigate('/admin/users')}>Manage Users</button>
              </>
            )}

            <span style={{
              color: 'var(--gold)',
              fontSize: '13px',
              fontFamily: 'var(--font-brand)',
              letterSpacing: '0.04em',
              padding: '4px 12px',
              border: '1px solid var(--gold)',
              borderRadius: '999px',
              opacity: 0.85,
            }}>
              ꧁ {user.firstName} ꧂
            </span>
            <button className="btn btn-danger" onClick={logoutUser}>Logout</button>
          </>
        )}

      </div>
    </nav>
  )
}

export default Navbar