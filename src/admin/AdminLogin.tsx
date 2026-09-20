import { LockKeyhole, LogIn } from 'lucide-react'
import { useEffect, useState } from 'react'\nimport type { FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { adminApi } from '../lib/adminApi'

export default function AdminLogin() {
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    adminApi.me().then(() => navigate('/admin', { replace: true })).catch(() => undefined)
  }, [navigate])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await adminApi.login(username, password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from || '/admin', { replace: true })
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to sign in.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="admin-login-page">
      <div className="admin-login-card">
        <div className="admin-login-brand">
          <span className="brand-mark">S</span>
          <div><strong>Swami Bags</strong><small>Private catalogue admin</small></div>
        </div>
        <div className="admin-login-icon"><LockKeyhole /></div>
        <h1>Welcome back</h1>
        <p>Sign in to add products, update stock and create AI marketing images.</p>

        <form onSubmit={submit}>
          <label>
            <span>Username</span>
            <input autoComplete="username" required value={username} onChange={(event) => setUsername(event.target.value)} />
          </label>
          <label>
            <span>Password</span>
            <input type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} />
          </label>
          {error && <div className="admin-error">{error}</div>}
          <button className="btn btn-primary btn-large btn-full" disabled={submitting}>
            <LogIn size={18} /> {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  )
}
