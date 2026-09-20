import { MessageCircle, PackageCheck, Phone, Truck } from 'lucide-react'
import { whatsappNumber } from '../data/products'

export default function Contact() {
  const message = 'Hello, I would like to discuss a wholesale bag requirement.'
  const whatsappHref = `https://wa.me/${whatsappNumber}?text=${encodeURIComponent(message)}`

  return (
    <section className="page-section contact-page">
      <div className="container contact-grid">
        <div className="contact-copy">
          <span className="kicker">Wholesale enquiries</span>
          <h1>Tell us what you need. We’ll keep the next step simple.</h1>
          <p>Share the bag type, expected quantity and any branding requirement. For a specific product, use the WhatsApp button on that product page so the product code is included automatically.</p>
          <a className="btn btn-whatsapp btn-large" href={whatsappHref} target="_blank" rel="noreferrer">
            <MessageCircle size={19} />
            Start WhatsApp enquiry
          </a>
        </div>

        <aside className="contact-card">
          <div className="contact-card-head">
            <span className="brand-mark">S</span>
            <div><strong>Swami Bags</strong><small>Wholesale enquiries only</small></div>
          </div>
          <div className="contact-row"><MessageCircle /><div><span>WhatsApp</span><strong>+91 98765 43210</strong></div></div>
          <div className="contact-row"><Phone /><div><span>Phone</span><strong>+91 98765 43210</strong></div></div>
          <div className="contact-row"><PackageCheck /><div><span>Ordering</span><strong>Bulk quantities / MOQ based</strong></div></div>
          <div className="contact-row"><Truck /><div><span>Supply</span><strong>Pan India</strong></div></div>
          <p className="contact-note">Phone number and business details are placeholders until the final client details are provided.</p>
        </aside>
      </div>
    </section>
  )
}