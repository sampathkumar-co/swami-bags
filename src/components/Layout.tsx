import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { Menu, MessageCircle, X } from 'lucide-react'
import { useCatalog } from '../context/catalog-context'
import { hasWhatsAppNumber, whatsappUrl } from '../lib/whatsapp'

const navItems = [
  ['Home', '/'],
  ['Products', '/products'],
  ['About Us', '/about'],
  ['Contact', '/contact'],
]

export default function Layout() {
  const [open, setOpen] = useState(false)
  const { config } = useCatalog()
  const hasWhatsApp = hasWhatsAppNumber(config.whatsappNumber)
  const phoneHref = config.businessPhone ? `tel:${config.businessPhone.replace(/[^0-9+]/g, '')}` : ''
  const enquiryHref = whatsappUrl(
    config.whatsappNumber,
    'Hello, I would like to know more about your wholesale bag catalogue.',
  )

  return (
    <div className="site-shell">
      <header className="header">
        <div className="container nav-wrap">
          <Link to="/" className="brand" onClick={() => setOpen(false)}>
            <span className="brand-mark">{config.logoUrl ? <img src={config.logoUrl} alt="" /> : 'NC'}</span>
            <span>
              <strong>{config.brandName || 'New Chandra Bags'}</strong>
              <small>Bags for a brighter tomorrow</small>
            </span>
          </Link>

          <nav id="primary-navigation" className={open ? 'nav-links is-open' : 'nav-links'} aria-label="Primary navigation">
            {navItems.map(([label, path]) => (
              <NavLink key={path} to={path} onClick={() => setOpen(false)}>
                {label}
              </NavLink>
            ))}
          </nav>

          <a className="btn btn-primary nav-cta" href={enquiryHref}>
            <MessageCircle size={17} />
            {hasWhatsApp ? 'Enquire on WhatsApp' : 'Wholesale enquiry'}
          </a>

          <button
            className="menu-button"
            aria-label={open ? 'Close navigation' : 'Open navigation'}
            aria-controls="primary-navigation"
            aria-expanded={open}
            onClick={() => setOpen((value) => !value)}
          >
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
              <span className="brand-mark">{config.logoUrl ? <img src={config.logoUrl} alt="" /> : 'NC'}</span>
              <span>
                <strong>{config.brandName || 'New Chandra Bags'}</strong>
                <small>Wholesale bags for growing businesses</small>
              </span>
            </Link>
            <p>Wholesale catalogue for cash bags, luggage, jute bags, zip bags and purses.</p>
          </div>
          <div>
            <h2 className="footer-heading">Explore</h2>
            <Link to="/products">Catalogue</Link>
            <Link to="/about">About us</Link>
            <Link to="/contact">Wholesale enquiry</Link>
          </div>
          <div>
            <h2 className="footer-heading">Categories</h2>
            <span>Cash Bags</span>
            <span>Luggage Bags</span>
            <span>Jute Bags</span>
            <span>Zip Bags</span>
            <span>Purses</span>
          </div>
          <div>
            <h2 className="footer-heading">Wholesale enquiries</h2>
            <a href={enquiryHref}>{hasWhatsApp ? 'WhatsApp us' : 'Contact us'}</a>
            {config.businessPhone && <a href={phoneHref}>{config.businessPhone}</a>}
            <span>Bulk orders welcome</span>
          </div>
        </div>
        <div className="container footer-bottom">
          <span>© {new Date().getFullYear()} {config.brandName || 'New Chandra Bags'}</span>
          <span>Wholesale · MOQ · Direct enquiries</span>
        </div>
      </footer>
    </div>
  )
}
