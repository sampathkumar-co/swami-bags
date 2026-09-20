import { ArrowRight, Boxes, Handshake, ShieldCheck, Truck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { products } from '../data/products'

export default function About() {
  return (
    <>
      <section className="about-hero">
        <div className="container about-hero-grid">
          <div>
            <span className="kicker">About Swami Bags</span>
            <h1>Wholesale bags built around long-term business relationships.</h1>
            <p>We keep the buying experience straightforward: dependable products, clear quantities, transparent availability and direct communication.</p>
            <Link className="btn btn-primary btn-large" to="/products">Explore catalogue <ArrowRight size={18} /></Link>
          </div>
          <div className="about-visual">
            <img src={products[2].image} alt="Swami Bags wholesale range" />
            <div className="about-visual-note">Good bags.<br />Better business.</div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container value-grid">
          <article><ShieldCheck /><h3>Consistent quality</h3><p>Practical materials and construction chosen for repeat wholesale use.</p></article>
          <article><Boxes /><h3>Bulk ready</h3><p>MOQ, available quantity and restock information are visible before you enquire.</p></article>
          <article><Truck /><h3>Pan India supply</h3><p>Designed for buyers who need dependable dispatch and recurring supply.</p></article>
          <article><Handshake /><h3>Direct relationship</h3><p>Product enquiries go straight to WhatsApp, keeping communication fast and human.</p></article>
        </div>
      </section>

      <section className="section section-soft">
        <div className="container story-panel story-panel-reverse">
          <div className="story-copy">
            <span className="kicker">Our approach</span>
            <h2>Professional without making wholesale complicated.</h2>
            <p>This website is intentionally catalogue-first rather than retail ecommerce. There is no customer account, payment flow or cluttered checkout. Buyers compare products, check material and availability, then send a direct enquiry with the correct product code already included.</p>
            <div className="story-stats">
              <div><strong>5</strong><span>Focused categories</span></div>
              <div><strong>Direct</strong><span>WhatsApp enquiries</span></div>
              <div><strong>Clear</strong><span>Stock & restock status</span></div>
            </div>
          </div>
          <div className="story-image">
            <img src={products[1].image} alt="Wholesale bag catalogue" loading="lazy" />
          </div>
        </div>
      </section>
    </>
  )
}