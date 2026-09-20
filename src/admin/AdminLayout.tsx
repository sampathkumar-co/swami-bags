import { Boxes, ExternalLink, LayoutDashboard, LogOut, Plus, Settings } from 'lucide-react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { adminApi } from '../lib/adminApi'

export default function AdminLayout() {
  const navigate = useNavigate()

  const logout = async () => {
    try {
      await adminApi.logout()
    } finally {
      navigate('/admin/login', { replace: true })
    }
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <Link className="admin-brand" to="/admin">
          <span className="brand-mark">S</span>
          <span><strong>Swami Bags</strong><small>Catalogue admin</small></span>
        </Link>

        <nav>
          <NavLink end to="/admin"><LayoutDashboard size={18} /> Dashboard</NavLink>
          <NavLink to="/admin/products/new"><Plus size={18} /> Add product</NavLink>
          <NavLink to="/admin/settings"><Settings size={18} /> Settings</NavLink>
          <a href="/" target="_blank" rel="noreferrer"><ExternalLink size={18} /> View website</a>
        </nav>

        <button className="admin-logout" onClick={logout}><LogOut size={18} /> Sign out</button>
      </aside>

      <div className="admin-main">
        <header className="admin-topbar">
          <div><Boxes size={18} /><span>Wholesale catalogue management</span></div>
          <a href="/" target="_blank" rel="noreferrer">Open public site <ExternalLink size={15} /></a>
        </header>
        <main className="admin-content"><Outlet /></main>
      </div>
    </div>
  )
}
