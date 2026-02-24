import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAllBooks, deleteBook } from '../../api/bookApi'

function AdminBooksPage() {
  const navigate = useNavigate()
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deletingId, setDeletingId] = useState(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState(null)

  useEffect(() => {
    getAllBooks()
      .then(res => setBooks(res.data))
      .catch(() => setError('Could not load books.'))
      .finally(() => setLoading(false))
  }, [])

  const handleDelete = async (bookId) => {
    setDeletingId(bookId)
    setConfirmDeleteId(null)
    try {
      await deleteBook(bookId)
      setBooks(prev => prev.filter(b => b.id !== bookId))
    } catch (err) {
      setError(err.response?.data?.message || 'Could not delete book.')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="page">
      <div className="container">

        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '8px',
        }}>
          <h1 style={{
            fontFamily: 'var(--font-heading)',
            color: 'var(--text-heading)',
            fontSize: '2rem',
            margin: 0,
          }}>
            Manage Books
          </h1>
          <button
            className="btn btn-primary"
            onClick={() => navigate('/admin/books/new')}
          >
            + Add Book
          </button>
        </div>

        <p style={{
          color: 'var(--text-medium)',
          fontFamily: 'var(--font-body)',
          marginBottom: '40px',
        }}>
          Add, edit, or remove books from the catalogue.
        </p>

        <div className="divider" style={{ marginBottom: '40px' }} />

        {loading && (
          <p style={{ color: 'var(--text-medium)', fontFamily: 'var(--font-body)' }}>
            Loading books...
          </p>
        )}

        {error && <p className="error-msg">{error}</p>}

        {!loading && !error && books.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {books.map(book => (
              <div key={book.id} className="card" style={{
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
                  <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <img
                      src={book.coverImageUrl || 'https://placehold.co/50x75?text=?'}
                      alt={book.title}
                      style={{
                        width: '50px',
                        height: '75px',
                        objectFit: 'cover',
                        borderRadius: '3px',
                        flexShrink: 0,
                      }}
                    />
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <h3 style={{
                        fontFamily: 'var(--font-heading)',
                        color: 'var(--text-heading)',
                        fontSize: '0.95rem',
                        margin: 0,
                      }}>
                        {book.title}
                      </h3>
                      <p style={{
                        color: 'var(--text-medium)',
                        fontFamily: 'var(--font-body)',
                        fontSize: '0.85rem',
                        margin: 0,
                      }}>
                        {book.author} · {book.pubYear}
                      </p>
                      <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                        {book.genre && (
                          <span className="badge badge-gold">{book.genre}</span>
                        )}
                        <span className={`badge ${book.copiesAvailable > 0 ? 'badge-green' : 'badge-red'}`}>
                          {book.copiesAvailable} copies
                        </span>
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button
                      className="btn btn-secondary-light"
                      onClick={() => navigate(`/admin/books/edit/${book.id}`)}
                    >
                      Edit
                    </button>
                    <button
                      className="btn btn-danger"
                      disabled={deletingId === book.id}
                      onClick={() => setConfirmDeleteId(book.id)}
                    >
                      {deletingId === book.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                </div>

                {confirmDeleteId === book.id && (
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
                      Delete <strong>{book.title}</strong>? This cannot be undone.
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
                        onClick={() => handleDelete(book.id)}
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

export default AdminBooksPage