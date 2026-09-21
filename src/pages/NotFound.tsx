import { ArrowLeft, MessageCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useCatalog } from '../context/catalog-context'
import { hasWhatsAppNumber, whatsappUrl } from '../lib/whatsapp'

export default function NotFound() {
  const { config } = useCatalog()
  const hasWhatsApp = hasWhatsAppNumber(config.whatsappNumber)
  const enquiry = whatsappUrl(
    config.whatsappNumber,
    'Hello, I could not find the page/product I was looking for. Please help me with the wholesale catalogue.',
  )

  return (
    <section className="page-section">
      <div className="container empty-state">
        <span className="kicker">404 · Page not found</span>
        <h1>This page is not in the catalogue.</h1>
        <p>The link may be old, or the product may no longer be published. You can return to the catalogue or ask us directly.</p>
        <div className="hero-actions">
          <Link className="btn btn-primary" to="/products"><ArrowLeft size={17} /> Browse catalogue</Link>
          <a className="btn btn-secondary" href={enquiry}><MessageCircle size={17} /> {hasWhatsApp ? 'Ask on WhatsApp' : 'Send an enquiry'}</a>
        </div>
      </div>
    </section>
  )
}
