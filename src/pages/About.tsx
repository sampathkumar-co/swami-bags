import { ArrowRight, Boxes, Handshake, ShieldCheck, Truck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useCatalog } from '../context/catalog-context'

export default function About() {
  const { products, config } = useCatalog()
  const visuals = products

  return (
    <>
      <section className="about-hero">
        <div className="container about-hero-grid">
          <div>
            <span className="kicker">About {config.brandName || 'New Chandra Bags'}</span>
            <h1>A straightforward wholesale bag catalogue.</h1>
            <p>We keep the buying workflow simple: clear product details, visible quantities, transparent availability and direct enquiries.</p>
            <Link className="btn btn-primary btn-large" to="/products">Explore catalogue <ArrowRight size={18} /></Link>
          </div>
          <div className="about-visual">
            {visuals[2]?.image
              ? <img src={visuals[2].image} alt="Wholesale bag range" />
              : <div className="product-placeholder">Real product photography will appear here.</div>}
            <div className="about-visual-note">Good bags.<br />Better business.</div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container value-grid">
          <article><ShieldCheck /><h2>Clear specifications</h2><p>Material, MOQ, stock and product details are shown before you enquire.</p></article>
          <article><Boxes /><h2>Bulk ready</h2><p>MOQ, available quantity and restock information are visible before you enquire.</p></article>
          <article><Truck /><h2>Dispatch clarity</h2><p>Confirm current availability, quantities and dispatch timing directly before ordering.</p></article>
          <article><Handshake /><h2>Direct enquiries</h2><p>Product codes and quantities are prepared for a simple buyer-to-business enquiry.</p></article>
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
              <div><strong>Direct</strong><span>Wholesale enquiries</span></div>
              <div><strong>Clear</strong><span>Stock & restock status</span></div>
            </div>
          </div>
          <div className="story-image">
            {visuals[1]?.image
              ? <img src={visuals[1].image} alt="Wholesale bag catalogue" loading="lazy" />
              : <div className="product-placeholder">Published catalogue imagery will appear here.</div>}
          </div>
        </div>
      </section>
    </>
  )
}
