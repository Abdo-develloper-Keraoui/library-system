import { useEffect, useState } from 'react'
import { getAllBooks } from '../api/bookApi'
import BookCard from '../components/BookCard'

function BooksPage() {
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [activeGenre, setActiveGenre] = useState('All')

  useEffect(() => {
    getAllBooks()
      .then(res => setBooks(res.data))
      .catch(() => setError('Could not load books. Please try again later.'))
      .finally(() => setLoading(false))
  }, [])

  const genres = ['All', ...new Set(books.map(b => b.genre).filter(Boolean))]

  const filteredBooks = activeGenre === 'All'
    ? books
    : books.filter(b => b.genre === activeGenre)

  return (
    <div className="page">
      <div className="container">
        <h1 style={{
          fontFamily: 'var(--font-heading)',
          color: 'var(--text-heading)',
          fontSize: '2rem',
          marginBottom: '8px',
        }}>
          The Collection
        </h1>
        <p style={{
          color: 'var(--text-medium)',
          fontFamily: 'var(--font-body)',
          marginBottom: '32px',
        }}>
          Browse our full catalogue. Click any book to learn more.
        </p>

        {/* Genre filter buttons */}
        {!loading && !error && books.length > 0 && (
          <div style={{
            display: 'flex',
            gap: '10px',
            flexWrap: 'wrap',
            marginBottom: '32px',
          }}>
            {genres.map(genre => (
              <button
                key={genre}
                onClick={() => setActiveGenre(genre)}
                style={{
                  fontFamily: 'var(--font-body)',
                  fontSize: '0.875rem',
                  padding: '6px 18px',
                  borderRadius: '20px',
                  border: '1px solid var(--border)',
                  cursor: 'pointer',
                  backgroundColor: activeGenre === genre ? 'var(--jade)' : 'var(--bg-card)',
                  color: activeGenre === genre ? '#fff' : 'var(--text-body)',
                  fontWeight: activeGenre === genre ? '600' : '400',
                  transition: 'all 0.2s ease',
                }}
              >
                {genre}
              </button>
            ))}
          </div>
        )}

        <div className="divider" style={{ marginBottom: '40px' }} />

        {loading && (
          <p style={{ color: 'var(--text-medium)', fontFamily: 'var(--font-body)' }}>
            Loading books...
          </p>
        )}
        {error && <p className="error-msg">{error}</p>}

        {!loading && !error && filteredBooks.length === 0 && (
          <div className="empty-state">
            <div className="icon">📭</div>
            <p>No books found in this category.</p>
          </div>
        )}

        {!loading && !error && filteredBooks.length > 0 && (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
            gap: '24px',
          }}>
            {filteredBooks.map(book => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default BooksPage