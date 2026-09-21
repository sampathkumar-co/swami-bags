import { Mail, MapPin, MessageCircle, PackageCheck, Phone, Send, Truck } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useCatalog } from '../context/catalog-context'
import { productCategories } from '../data/products'
import { hasWhatsAppNumber, whatsappUrl } from '../lib/whatsapp'

export default function Contact() {
  const { config } = useCatalog()
  const [name, setName] = useState('')
  const [category, setCategory] = useState(productCategories[0])
  const [quantity, setQuantity] = useState(100)
  const [note, setNote] = useState('')
  const hasWhatsApp = hasWhatsAppNumber(config.whatsappNumber)
  const phoneHref = config.businessPhone ? `tel:${config.businessPhone.replace(/[^0-9+]/g, '')}` : ''
  const hasContactDetails = hasWhatsApp || Boolean(config.businessPhone || config.businessEmail || config.businessAddress)

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
          <p>Share the bag type, expected quantity and any branding requirement. {hasWhatsApp ? 'We prepare the WhatsApp message automatically so you can send a complete enquiry in one tap.' : 'Use the available contact details to send a complete wholesale enquiry.'}</p>

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
                <input type="number" min={1} step={1} value={quantity} onChange={(event) => setQuantity(Math.max(1, Math.floor(Number(event.target.value) || 1)))} />
              </label>
            </div>

            <label>
              <span>Requirement <small>optional</small></span>
              <textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="Example: preferred colour, target quantity, delivery timing or other requirements..." rows={4} />
            </label>

            {hasWhatsApp ? (
              <a className="btn btn-whatsapp btn-large" href={enquiryHref}>
                <Send size={18} />
                Prepare enquiry on WhatsApp
              </a>
            ) : (
              <div className="contact-unavailable" role="status">
                WhatsApp enquiries are not configured right now. {hasContactDetails ? 'Please use the available contact details.' : 'Contact details are being updated.'}
              </div>
            )}
          </div>
        </div>

        <aside className="contact-card">
          <div className="contact-card-head">
            <span className="brand-mark">{config.logoUrl ? <img src={config.logoUrl} alt="" /> : 'NC'}</span>
            <div><strong>{config.brandName || 'New Chandra Bags'}</strong><small>Wholesale enquiries only</small></div>
          </div>
          {hasWhatsApp && (
            <div className="contact-row"><MessageCircle /><div><span>WhatsApp</span><strong>+{config.whatsappNumber.replace(/\D/g, '')}</strong></div></div>
          )}
          {config.businessPhone && (
            <div className="contact-row"><Phone /><div><span>Phone</span><a href={phoneHref}><strong>{config.businessPhone}</strong></a></div></div>
          )}
          {config.businessEmail && (
            <div className="contact-row"><Mail /><div><span>Email</span><a href={`mailto:${config.businessEmail}`}><strong>{config.businessEmail}</strong></a></div></div>
          )}
          {config.businessAddress && (
            <div className="contact-row"><MapPin /><div><span>Address</span><strong>{config.businessAddress}</strong></div></div>
          )}
          <div className="contact-row"><PackageCheck /><div><span>Ordering</span><strong>Bulk quantities / MOQ based</strong></div></div>
          <div className="contact-row"><Truck /><div><span>Dispatch</span><strong>Confirm timing on enquiry</strong></div></div>
        </aside>
      </div>
    </section>
  )
}
