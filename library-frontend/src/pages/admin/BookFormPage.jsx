import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createBook, getBookById, updateBook } from '../../api/bookApi'

function BookFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEditing = Boolean(id)

  const [form, setForm] = useState({
    title: '',
    author: '',
    isbn: '',
    pubYear: '',
    copiesAvailable: '',
    genre: '',
    coverImageUrl: '',
  })

  const [loading, setLoading] = useState(false)
  const [pageLoading, setPageLoading] = useState(isEditing)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!isEditing) return
    getBookById(id)
      .then(res => setForm({
        title: res.data.title || '',
        author: res.data.author || '',
        isbn: res.data.isbn || '',
        pubYear: res.data.pubYear || '',
        copiesAvailable: res.data.copiesAvailable || '',
        genre: res.data.genre || '',
        coverImageUrl: res.data.coverImageUrl || '',
      }))
      .catch(() => setError('Could not load book data.'))
      .finally(() => setPageLoading(false))
  }, [id])

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const payload = {
        ...form,
        pubYear: parseInt(form.pubYear),
        copiesAvailable: parseInt(form.copiesAvailable),
      }
      if (isEditing) {
        await updateBook(id, payload)
      } else {
        await createBook(payload)
      }
      navigate('/admin/books')
    } catch (err) {
      setError(err.response?.data?.message || 'Something went wrong.')
    } finally {
      setLoading(false)
    }
  }

  if (pageLoading) return (
    <div className="page">
      <div className="container">
        <p style={{ color: 'var(--text-medium)' }}>Loading book data...</p>
      </div>
    </div>
  )

  return (
    <div className="page">
      <div className="container" style={{ maxWidth: '560px' }}>

        <button
          className="btn btn-secondary-light"
          onClick={() => navigate('/admin/books')}
          style={{ marginBottom: '32px' }}
        >
          ← Back to Manage Books
        </button>

        <h1 style={{
          fontFamily: 'var(--font-heading)',
          color: 'var(--text-heading)',
          fontSize: '2rem',
          marginBottom: '8px',
        }}>
          {isEditing ? 'Edit Book' : 'Add New Book'}
        </h1>

        <p style={{
          color: 'var(--text-medium)',
          fontFamily: 'var(--font-body)',
          marginBottom: '40px',
        }}>
          {isEditing ? 'Update the details below.' : 'Fill in the details to add a book to the catalogue.'}
        </p>

        <div className="divider" style={{ marginBottom: '40px' }} />

        {error && <p className="error-msg" style={{ marginBottom: '24px' }}>{error}</p>}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

          <div className="form-group">
            <label>Title</label>
            <input
              name="title"
              value={form.title}
              onChange={handleChange}
              placeholder="e.g. Crime and Punishment"
              required
            />
          </div>

          <div className="form-group">
            <label>Author</label>
            <input
              name="author"
              value={form.author}
              onChange={handleChange}
              placeholder="e.g. Fyodor Dostoevsky"
              required
            />
          </div>

          <div className="form-group">
            <label>ISBN</label>
            <input
              name="isbn"
              value={form.isbn}
              onChange={handleChange}
              placeholder="e.g. 9780143107"
              required
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            <div className="form-group">
              <label>Publication Year</label>
              <input
                name="pubYear"
                type="number"
                value={form.pubYear}
                onChange={handleChange}
                placeholder="e.g. 1866"
                required
              />
            </div>

            <div className="form-group">
              <label>Copies Available</label>
              <input
                name="copiesAvailable"
                type="number"
                value={form.copiesAvailable}
                onChange={handleChange}
                placeholder="e.g. 3"
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label>Genre <span style={{ color: 'var(--text-light)', fontSize: '0.85rem' }}>(optional)</span></label>
            <input
              name="genre"
              value={form.genre}
              onChange={handleChange}
              placeholder="e.g. Fiction, History, Self-Help"
            />
          </div>

          <div className="form-group">
            <label>Cover Image URL <span style={{ color: 'var(--text-light)', fontSize: '0.85rem' }}>(optional)</span></label>
            <input
              name="coverImageUrl"
              value={form.coverImageUrl}
              onChange={handleChange}
              placeholder="https://..."
            />
          </div>

          {form.coverImageUrl && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <img
                src={form.coverImageUrl}
                alt="Cover preview"
                style={{
                  width: '80px',
                  height: '120px',
                  objectFit: 'cover',
                  borderRadius: '4px',
                  border: '1px solid var(--border)',
                }}
                onError={e => e.target.style.display = 'none'}
              />
              <p style={{ color: 'var(--text-medium)', fontSize: '0.85rem', fontFamily: 'var(--font-body)' }}>
                Cover preview
              </p>
            </div>
          )}

          <div className="divider" />

          <div style={{ display: 'flex', gap: '12px' }}>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Saving...' : isEditing ? 'Save Changes' : 'Add Book'}
            </button>
            <button
              type="button"
              className="btn btn-secondary-light"
              onClick={() => navigate('/admin/books')}
            >
              Cancel
            </button>
          </div>

        </form>
      </div>
    </div>
  )
}

export default BookFormPage