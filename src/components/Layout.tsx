import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { Menu, MessageCircle, X } from 'lucide-react'
import { useCatalog } from '../context/CatalogContext'
import { whatsappUrl } from '../lib/whatsapp'

const navItems = [
  ['Home', '/'],
  ['Products', '/products'],
  ['About Us', '/about'],
  ['Contact', '/contact'],
]

export default function Layout() {
  const [open, setOpen] = useState(false)
  const { config } = useCatalog()
  const enquiryHref = whatsappUrl(
    config.whatsappNumber,
    'Hello, I would like to know more about your wholesale bag catalogue.',
  )

  return (
    <div className="site-shell">
      <header className="header">
        <div className="container nav-wrap">
          <Link to="/" className="brand" onClick={() => setOpen(false)}>
            <span className="brand-mark">NC</span>
            <span>
              <strong>{config.brandName || 'New Chandra Bags'}</strong>
              <small>Bags for a brighter tomorrow</small>
            </span>
          </Link>

          <nav className={open ? 'nav-links is-open' : 'nav-links'} aria-label="Primary navigation">
            {navItems.map(([label, path]) => (
              <NavLink key={path} to={path} onClick={() => setOpen(false)}>
                {label}
              </NavLink>
            ))}
          </nav>

          <a className="btn btn-primary nav-cta" href={enquiryHref}>
            <MessageCircle size={17} />
            Enquire on WhatsApp
          </a>

          <button className="menu-button" aria-label="Toggle navigation" onClick={() => setOpen((value) => !value)}>
            {open ? <X /> : <Menu />}
          </button>
        </div>
      </header>

      <main>
        <Outlet />
      </main>

      <footer className="footer">
        <div className="container footer-grid">
          <div>
            <Link to="/" className="brand footer-brand">
              <span className="brand-mark">NC</span>
              <span>
                <strong>{config.brandName || 'New Chandra Bags'}</strong>
                <small>Wholesale bags for growing businesses</small>
              </span>
            </Link>
            <p>Simple, reliable wholesale supply across cash bags, luggage, jute bags, zip bags and purses.</p>
          </div>
          <div>
            <h4>Explore</h4>
            <Link to="/products">Catalogue</Link>
            <Link to="/about">About us</Link>
            <Link to="/contact">Wholesale enquiry</Link>
          </div>
          <div>
            <h4>Categories</h4>
            <span>Cash Bags</span>
            <span>Luggage Bags</span>
            <span>Jute Bags</span>
            <span>Zip Bags</span>
            <span>Purses</span>
          </div>
          <div>
            <h4>Wholesale enquiries</h4>
            <a href={enquiryHref}>WhatsApp us</a>
            {config.businessPhone && <span>{config.businessPhone}</span>}
            <span>Bulk orders welcome</span>
          </div>
        </div>
        <div className="container footer-bottom">
          <span>© {new Date().getFullYear()} {config.brandName || 'New Chandra Bags'}</span>
          <span>Quality · Trust · Long-term partnerships</span>
        </div>
      </footer>
    </div>
  )
}
