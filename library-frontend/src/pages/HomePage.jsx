import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getAllBooks } from '../api/bookApi'

function HomePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [books, setBooks] = useState([])

  useEffect(() => {
    getAllBooks()
      .then(res => setBooks(res.data))
      .catch(() => setBooks([]))
  }, [])

  const belt = [...books, ...books]

  return (
    <>
      <style>{`
        @keyframes scrollBelt {
          0%   { transform: translateX(0); }
          100% { transform: translateX(-50%); }
        }
        .belt-track {
          display: flex;
          gap: 24px;
          animation: scrollBelt 40s linear infinite;
          width: max-content;
        }
        .belt-track:hover {
          animation-play-state: paused;
        }
      `}</style>

      <div style={{ overflow: 'hidden' }}>

        {/* ── HERO SECTION ── */}
        <div style={{
          backgroundColor: 'var(--bg-section)',
          textAlign: 'center',
          padding: '100px 24px 80px',
          borderBottom: '2px solid var(--border)',
        }}>
          <h1 style={{
            fontFamily: 'var(--font-brand)',
            color: 'var(--gold)',
            fontSize: '3.5rem',
            letterSpacing: '0.08em',
            marginBottom: '20px',
          }}>
            Rivendell Reads
          </h1>

          <p style={{
            fontFamily: 'var(--font-heading)',
            color: 'var(--text-medium)',
            fontSize: '1.15rem',
            fontStyle: 'italic',
            marginBottom: '48px',
          }}>
            "So many books, so little time." — Frank Zappa
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '16px' }}>
            {!user ? (
              <>
                <button className="btn btn-primary" onClick={() => navigate('/login')}>
                  Login
                </button>
                <button className="btn btn-secondary-light" onClick={() => navigate('/register')}>
                  Register
                </button>
              </>
            ) : (
              <button className="btn btn-primary" onClick={() => navigate('/books')}>
                Browse Books
              </button>
            )}
          </div>
        </div>

        {/* ── BOOK BELT SECTION ── */}
        {books.length > 0 && (
          <div style={{
            padding: '56px 0',
            backgroundColor: 'var(--bg-page)',
            overflow: 'hidden',
          }}>
            <h2 style={{
              fontFamily: 'var(--font-heading)',
              color: 'var(--text-heading)',
              textAlign: 'center',
              marginBottom: '36px',
              fontSize: '1.3rem',
            }}>
              From our collection
            </h2>

            <div style={{ overflow: 'hidden', cursor: 'pointer' }} onClick={() => navigate('/books')}>
              <div className="belt-track">
                {belt.map((book, index) =>
                  book.coverImageUrl ? (
                    <div key={index} style={{ flexShrink: 0 }}>
                      <img
                        src={book.coverImageUrl}
                        alt={book.title}
                        style={{
                          width: '120px',
                          height: '180px',
                          objectFit: 'cover',
                          borderRadius: '4px',
                          boxShadow: '0 4px 16px rgba(0,0,0,0.18)',
                        }}
                      />
                    </div>
                  ) : null
                )}
              </div>
            </div>
          </div>
        )}

      </div>
    </>
  )
}

export default HomePage