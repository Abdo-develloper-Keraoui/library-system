import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getBookById } from '../api/bookApi'
import { borrowBook } from '../api/borrowApi'
import { useAuth } from '../context/AuthContext'

function BookDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [book, setBook] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [borrowLoading, setBorrowLoading] = useState(false)
  const [borrowError, setBorrowError] = useState(null)
  const [borrowSuccess, setBorrowSuccess] = useState(null)

  useEffect(() => {
    getBookById(id)
      .then(res => setBook(res.data))
      .catch(() => setError('Could not load this book.'))
      .finally(() => setLoading(false))
  }, [id])

  const handleBorrow = async () => {
    setBorrowLoading(true)
    setBorrowError(null)
    setBorrowSuccess(null)
    try {
      await borrowBook(id)
      setBorrowSuccess('Book borrowed successfully! Check My Borrows for your due date.')
      setBook(prev => ({ ...prev, copiesAvailable: prev.copiesAvailable - 1 }))
    } catch (err) {
      setBorrowError(err.response?.data?.message || 'Something went wrong.')
    } finally {
      setBorrowLoading(false)
    }
  }

  if (loading) return (
    <div className="page">
      <div className="container">
        <p style={{ color: 'var(--text-medium)' }}>Loading...</p>
      </div>
    </div>
  )

  if (error) return (
    <div className="page">
      <div className="container">
        <p className="error-msg">{error}</p>
      </div>
    </div>
  )

  return (
    <div className="page">
      <div className="container">

        <button
          className="btn btn-secondary-light"
          onClick={() => navigate('/books')}
          style={{ marginBottom: '32px' }}
        >
          ← Back to catalogue
        </button>

        <div style={{
          display: 'flex',
          gap: '48px',
          alignItems: 'flex-start',
        }}>

          {/* LEFT — cover image */}
          <img
            src={book.coverImageUrl || 'https://placehold.co/200x300?text=No+Cover'}
            alt={book.title}
            style={{
              width: '200px',
              height: '300px',
              objectFit: 'cover',
              borderRadius: '6px',
              boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
              flexShrink: 0,
            }}
          />

          {/* RIGHT — book info */}
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '16px' }}>

            <h1 style={{
              fontFamily: 'var(--font-heading)',
              color: 'var(--text-heading)',
              fontSize: '2rem',
              margin: 0,
            }}>
              {book.title}
            </h1>

            <p style={{
              color: 'var(--text-medium)',
              fontFamily: 'var(--font-body)',
              fontSize: '1.05rem',
              margin: 0,
            }}>
              by {book.author}
            </p>

            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              {book.genre && (
                <span className="badge badge-gold">{book.genre}</span>
              )}
              <span className={`badge ${book.copiesAvailable > 0 ? 'badge-green' : 'badge-red'}`}>
                {book.copiesAvailable > 0 ? `${book.copiesAvailable} available` : 'Unavailable'}
              </span>
            </div>

            <div className="divider" />

            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <p style={{ color: 'var(--text-body)', fontFamily: 'var(--font-body)', margin: 0 }}>
                <strong>ISBN:</strong> {book.isbn}
              </p>
              <p style={{ color: 'var(--text-body)', fontFamily: 'var(--font-body)', margin: 0 }}>
                <strong>Published:</strong> {book.pubYear}
              </p>
            </div>

            <div className="divider" />

            {borrowSuccess && <p className="success-msg">{borrowSuccess}</p>}
            {borrowError && <p className="error-msg">{borrowError}</p>}

            <button
              className="btn btn-primary"
              disabled={book.copiesAvailable === 0 || !user || borrowLoading}
              onClick={handleBorrow}
              style={{ width: 'fit-content' }}
            >
              {!user
                ? 'Login to borrow'
                : borrowLoading
                ? 'Borrowing...'
                : book.copiesAvailable === 0
                ? 'Unavailable'
                : 'Borrow Book'
              }
            </button>

            {!user && (
              <p style={{ color: 'var(--text-light)', fontSize: '0.85rem', fontFamily: 'var(--font-body)' }}>
                <span
                  style={{ color: 'var(--jade)', cursor: 'pointer' }}
                  onClick={() => navigate('/login')}
                >
                  Login
                </span>
                {' '}or{' '}
                <span
                  style={{ color: 'var(--jade)', cursor: 'pointer' }}
                  onClick={() => navigate('/register')}
                >
                  register
                </span>
                {' '}to borrow books from our collection.
              </p>
            )}

          </div>
        </div>
      </div>
    </div>
  )
}

export default BookDetailPage