import { MessageCircle, PackageCheck, Phone, Send, Truck } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useCatalog } from '../context/CatalogContext'
import { productCategories } from '../data/products'
import { whatsappUrl } from '../lib/whatsapp'

export default function Contact() {
  const { config } = useCatalog()
  const [name, setName] = useState('')
  const [category, setCategory] = useState(productCategories[0])
  const [quantity, setQuantity] = useState(100)
  const [note, setNote] = useState('')

  const enquiryHref = useMemo(() => {
    const lines = [
      'Hello, I would like to discuss a wholesale bag requirement.',
      name.trim() ? `Name: ${name.trim()}` : '',
      `Category: ${category}`,
      `Approx. quantity: ${Math.max(1, quantity)} pieces`,
      note.trim() ? `Requirement: ${note.trim()}` : '',
      'Please share suitable products, pricing and availability. Thank you.',
    ].filter(Boolean)
    return whatsappUrl(config.whatsappNumber, lines.join('\n'))
  }, [name, category, quantity, note, config.whatsappNumber])

  return (
    <section className="page-section contact-page">
      <div className="container contact-grid">
        <div className="contact-copy">
          <span className="kicker">Wholesale enquiries</span>
          <h1>Tell us what you need. We’ll keep the next step simple.</h1>
          <p>Share the bag type, expected quantity and any branding requirement. We prepare the WhatsApp message automatically so you can send a complete enquiry in one tap.</p>

          <div className="enquiry-form">
            <label>
              <span>Your name <small>optional</small></span>
              <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Business / contact name" />
            </label>

            <div className="form-row">
              <label>
                <span>Bag category</span>
                <select value={category} onChange={(event) => setCategory(event.target.value as typeof category)}>
                  {productCategories.map((item) => <option key={item}>{item}</option>)}
                </select>
              </label>
              <label>
                <span>Approx. quantity</span>
                <input type="number" min={1} value={quantity} onChange={(event) => setQuantity(Math.max(1, Number(event.target.value) || 1))} />
              </label>
            </div>

            <label>
              <span>Requirement <small>optional</small></span>
              <textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="Example: need logo printing, red colour, delivery by next week..." rows={4} />
            </label>

            <a className="btn btn-whatsapp btn-large" href={enquiryHref}>
              <Send size={18} />
              Prepare enquiry on WhatsApp
            </a>
          </div>
        </div>

        <aside className="contact-card">
          <div className="contact-card-head">
            <span className="brand-mark">S</span>
            <div><strong>{config.brandName || 'Swami Bags'}</strong><small>Wholesale enquiries only</small></div>
          </div>
          <div className="contact-row"><MessageCircle /><div><span>WhatsApp</span><strong>{config.whatsappNumber ? `+${config.whatsappNumber}` : 'Add number before launch'}</strong></div></div>
          <div className="contact-row"><Phone /><div><span>Phone</span><strong>{config.businessPhone || 'Add phone before launch'}</strong></div></div>
          <div className="contact-row"><PackageCheck /><div><span>Ordering</span><strong>Bulk quantities / MOQ based</strong></div></div>
          <div className="contact-row"><Truck /><div><span>Supply</span><strong>Pan India</strong></div></div>
          {config.businessAddress && <p className="contact-note">{config.businessAddress}</p>}
        </aside>
      </div>
    </section>
  )
}
