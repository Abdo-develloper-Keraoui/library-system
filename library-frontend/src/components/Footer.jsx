import { useNavigate } from 'react-router-dom'

function Footer() {
  const navigate = useNavigate()
  const year = new Date().getFullYear()

  return (
    <footer style={{
      backgroundColor: 'var(--bg-navbar)',
      borderTop: '2px solid var(--gold)',
      color: 'var(--text-navbar)',
      fontFamily: 'var(--font-body)',
      marginTop: 'auto',
    }}>
      <div style={{
        maxWidth: '1100px',
        margin: '0 auto',
        padding: 'var(--space-lg) var(--space-md)',
        display: 'grid',
        gridTemplateColumns: '1fr 1fr 1fr',
        gap: 'var(--space-lg)',
        alignItems: 'start',
      }}>

        {/* LEFT — Brand */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <span
            onClick={() => navigate('/')}
            style={{
              fontFamily: 'var(--font-brand)',
              fontWeight: 700,
              fontSize: '1.1rem',
              color: 'var(--gold)',
              letterSpacing: '0.05em',
              cursor: 'pointer',
            }}
          >
            Rivendell Reads
          </span>
          <p style={{
            fontSize: '13px',
            color: 'rgba(232,220,200,0.6)',
            margin: 0,
            lineHeight: '1.6',
            fontStyle: 'italic',
          }}>
            "Fantasy is escapist, and that is its glory." — J.R.R. Tolkien
          </p>
        </div>

        {/* CENTER — Navigation */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <span style={{
            fontSize: '11px',
            fontWeight: 700,
            letterSpacing: '0.1em',
            textTransform: 'uppercase',
            color: 'var(--gold)',
          }}>
            Explore
          </span>
          {[
            { label: 'Home', path: '/' },
            { label: 'Browse Books', path: '/books' },
            { label: 'My Borrows', path: '/my-borrows' },
          ].map(({ label, path }) => (
            <span
              key={path}
              onClick={() => navigate(path)}
              style={{
                fontSize: '13px',
                color: 'rgba(232,220,200,0.75)',
                cursor: 'pointer',
                transition: 'color 0.15s',
              }}
              onMouseEnter={e => e.target.style.color = 'var(--gold)'}
              onMouseLeave={e => e.target.style.color = 'rgba(232,220,200,0.75)'}
            >
              {label}
            </span>
          ))}
        </div>

        {/* RIGHT — Tagline / Quote */}
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
          textAlign: 'right',
        }}>
          <span style={{
            fontSize: '11px',
            fontWeight: 700,
            letterSpacing: '0.1em',
            textTransform: 'uppercase',
            color: 'var(--gold)',
          }}>
            A Word from Rivendell
          </span>
          <p style={{
            fontSize: '12px',
            fontStyle: 'italic',
            color: 'rgba(232,220,200,0.55)',
            margin: 0,
            lineHeight: '1.7',
          }}>
            'All that is gold does not glitter' — but a well-worn book always shines.
          </p>
        </div>
      </div>

      {/* Bottom bar */}
      <div style={{
        borderTop: '1px solid rgba(196,154,60,0.2)',
        padding: 'var(--space-sm) var(--space-md)',
        textAlign: 'center',
        fontSize: '12px',
        color: 'rgba(232,220,200,0.4)',
        fontFamily: 'var(--font-body)',
      }}>
        © {year} Rivendell Reads. All rights reserved.
      </div>
    </footer>
  )
}

export default Footer