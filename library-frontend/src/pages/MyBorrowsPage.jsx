import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyBorrows, returnBook } from '../api/borrowApi'

function MyBorrowsPage() {
  const navigate = useNavigate()
  const [borrows, setBorrows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [returningId, setReturningId] = useState(null)
  const [returnError, setReturnError] = useState(null)

  useEffect(() => {
    getMyBorrows()
      .then(res => setBorrows(res.data))
      .catch(() => setError('Could not load your borrows.'))
      .finally(() => setLoading(false))
  }, [])

  const handleReturn = async (borrowId) => {
    setReturningId(borrowId)
    setReturnError(null)
    try {
      await returnBook(borrowId)
      setBorrows(prev =>
        prev.map(b =>
          b.id === borrowId ? { ...b, status: 'RETURNED' } : b
        )
      )
    } catch (err) {
      setReturnError(err.response?.data?.message || 'Could not return book.')
    } finally {
      setReturningId(null)
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
          My Borrows
        </h1>

        <p style={{
          color: 'var(--text-medium)',
          fontFamily: 'var(--font-body)',
          marginBottom: '40px',
        }}>
          Your full borrowing history.
        </p>

        <div className="divider" style={{ marginBottom: '40px' }} />

        {loading && (
          <p style={{ color: 'var(--text-medium)', fontFamily: 'var(--font-body)' }}>
            Loading your borrows...
          </p>
        )}

        {error && <p className="error-msg">{error}</p>}
        {returnError && <p className="error-msg">{returnError}</p>}

        {!loading && !error && borrows.length === 0 && (
          <div className="empty-state">
            <div className="icon">📭</div>
            <p>You haven't borrowed any books yet.</p>
            <button
              className="btn btn-primary"
              onClick={() => navigate('/books')}
            >
              Browse catalogue
            </button>
          </div>
        )}

        {!loading && !error && borrows.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {borrows.map(borrow => (
              <div key={borrow.id} className="card" style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '16px',
              }}>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <h3 style={{
                    fontFamily: 'var(--font-heading)',
                    color: 'var(--text-heading)',
                    fontSize: '1rem',
                    margin: 0,
                  }}>
                    {borrow.bookTitle}
                  </h3>
                  <p style={{
                    color: 'var(--text-medium)',
                    fontFamily: 'var(--font-body)',
                    fontSize: '0.85rem',
                    margin: 0,
                  }}>
                    Borrowed: {borrow.borrowDate} &nbsp;|&nbsp; Due: {borrow.dueDate}
                  </p>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <span className={`badge ${borrow.status === 'ACTIVE' ? 'badge-green' : 'badge-grey'}`}>
                    {borrow.status}
                  </span>

                  {borrow.status === 'ACTIVE' && (
                    <button
                      className="btn btn-danger"
                      disabled={returningId === borrow.id}
                      onClick={() => handleReturn(borrow.id)}
                    >
                      {returningId === borrow.id ? 'Returning...' : 'Return'}
                    </button>
                  )}
                </div>

              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  )
}

export default MyBorrowsPage