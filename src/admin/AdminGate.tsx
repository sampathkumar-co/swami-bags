import { useEffect, useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { adminApi } from '../lib/adminApi'

export default function AdminGate() {
  const [state, setState] = useState<'checking' | 'allowed' | 'denied'>('checking')
  const location = useLocation()

  useEffect(() => {
    let active = true
    adminApi.me()
      .then((session) => {
        if (active) setState(session.authenticated ? 'allowed' : 'denied')
      })
      .catch(() => { if (active) setState('denied') })
    return () => { active = false }
  }, [])

  if (state === 'checking') {
    return <div className="admin-loading">Checking admin session…</div>
  }
  if (state === 'denied') {
    return <Navigate to="/admin/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
