import { useEffect, useState } from 'react'
import { getAllUsers, toggleSuspend, deleteUser } from '../../api/adminApi'

function AdminUsersPage() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [suspendingId, setSuspendingId] = useState(null)
  const [deletingId, setDeletingId] = useState(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState(null)

  useEffect(() => {
    getAllUsers()
      .then(res => setUsers(res.data))
      .catch(() => setError('Could not load users.'))
      .finally(() => setLoading(false))
  }, [])

  const handleToggleSuspend = async (userId) => {
    setSuspendingId(userId)
    try {
      const res = await toggleSuspend(userId)
      setUsers(prev =>
        prev.map(u => u.id === userId ? { ...u, active: res.data.active } : u)
      )
    } catch (err) {
      setError(err.response?.data?.message || 'Could not update user.')
    } finally {
      setSuspendingId(null)
    }
  }

  const handleDelete = async (userId) => {
    setDeletingId(userId)
    setConfirmDeleteId(null)
    try {
      await deleteUser(userId)
      setUsers(prev => prev.filter(u => u.id !== userId))
    } catch (err) {
      setError(err.response?.data?.message || 'Could not delete user.')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="page">
      <div className="container">

        <h1 style={{
          fontFamily: 'var(--font-heading)',
          color: 'var(--text-heading)',
          fontSize: '2rem',
          marginBottom: '8px',
        }}>
          Manage Users
        </h1>

        <p style={{
          color: 'var(--text-medium)',
          fontFamily: 'var(--font-body)',
          marginBottom: '40px',
        }}>
          Suspend or remove users from the system.
        </p>

        <div className="divider" style={{ marginBottom: '40px' }} />

        {loading && (
          <p style={{ color: 'var(--text-medium)', fontFamily: 'var(--font-body)' }}>
            Loading users...
          </p>
        )}

        {error && <p className="error-msg">{error}</p>}

        {!loading && !error && users.length === 0 && (
          <div className="empty-state">
            <div className="icon">👤</div>
            <p>No users found.</p>
          </div>
        )}

        {!loading && !error && users.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {users.map(user => (
              <div key={user.id} className="card" style={{
                display: 'flex',
                flexDirection: 'column',
                gap: '12px',
              }}>

                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '16px',
                }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    <h3 style={{
                      fontFamily: 'var(--font-heading)',
                      color: 'var(--text-heading)',
                      fontSize: '0.95rem',
                      margin: 0,
                    }}>
                      {user.firstName} {user.lastName}
                    </h3>
                    <p style={{
                      color: 'var(--text-medium)',
                      fontFamily: 'var(--font-body)',
                      fontSize: '0.85rem',
                      margin: 0,
                    }}>
                      {user.email}
                    </p>
                    <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                      <span className="badge badge-gold">{user.role}</span>
                      <span className={`badge ${user.active ? 'badge-green' : 'badge-red'}`}>
                        {user.active ? 'Active' : 'Suspended'}
                      </span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button
                      className="btn btn-secondary-light"
                      disabled={suspendingId === user.id}
                      onClick={() => handleToggleSuspend(user.id)}
                    >
                      {suspendingId === user.id
                        ? 'Updating...'
                        : user.active ? 'Suspend' : 'Unsuspend'
                      }
                    </button>
                    <button
                      className="btn btn-danger"
                      disabled={deletingId === user.id}
                      onClick={() => setConfirmDeleteId(user.id)}
                    >
                      {deletingId === user.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                </div>

                {/* Inline confirmation — only shows for the card being deleted */}
                {confirmDeleteId === user.id && (
                  <div style={{
                    backgroundColor: 'var(--bg-section)',
                    border: '1px solid var(--border)',
                    borderRadius: '8px',
                    padding: '12px 16px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '16px',
                    flexWrap: 'wrap',
                  }}>
                    <p style={{
                      fontFamily: 'var(--font-body)',
                      color: 'var(--text-body)',
                      fontSize: '0.875rem',
                      margin: 0,
                    }}>
                      Delete <strong>{user.firstName}</strong>? This cannot be undone.
                    </p>
                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button
                        className="btn btn-secondary-light"
                        onClick={() => setConfirmDeleteId(null)}
                      >
                        Cancel
                      </button>
                      <button
                        className="btn btn-danger"
                        onClick={() => handleDelete(user.id)}
                      >
                        Yes, delete
                      </button>
                    </div>
                  </div>
                )}

              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  )
}

export default AdminUsersPage