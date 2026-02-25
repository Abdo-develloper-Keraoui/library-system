import { useNavigate } from 'react-router-dom'

function BookCard({ book }) {
  const navigate = useNavigate()

  return (
    <div
      className="card"
      onClick={() => navigate(`/books/${book.id}`)}
      style={{
        cursor: 'pointer',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        transition: 'transform 0.2s ease, box-shadow 0.2s ease',
      }}
      onMouseEnter={e => {
        e.currentTarget.style.transform = 'translateY(-4px)'
        e.currentTarget.style.boxShadow = '0 8px 24px rgba(0,0,0,0.12)'
      }}
      onMouseLeave={e => {
        e.currentTarget.style.transform = 'translateY(0)'
        e.currentTarget.style.boxShadow = ''
      }}
    >
      <img
        src={book.coverImageUrl || 'https://placehold.co/200x300?text=No+Cover'}
        alt={book.title}
        style={{
          width: '100%',
          height: '220px',
          objectFit: 'cover',
          borderRadius: '4px',
        }}
      />

      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', flex: 1 }}>
        <h3 style={{
          fontFamily: 'var(--font-heading)',
          color: 'var(--text-heading)',
          fontSize: '0.95rem',
          margin: 0,
          lineHeight: '1.3',
        }}>
          {book.title}
        </h3>

        <p style={{
          color: 'var(--text-medium)',
          fontSize: '0.85rem',
          margin: 0,
          fontFamily: 'var(--font-body)',
        }}>
          {book.author}
        </p>

        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: 'auto' }}>
          {book.genre && (
            <span className="badge badge-gold">{book.genre}</span>
          )}
          <span className={`badge ${book.copiesAvailable > 0 ? 'badge-green' : 'badge-red'}`}>
            {book.copiesAvailable > 0 ? `${book.copiesAvailable} available` : 'Unavailable'}
          </span>
        </div>
      </div>
    </div>
  )
}

export default BookCard