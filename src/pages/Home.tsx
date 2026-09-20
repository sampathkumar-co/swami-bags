import { ArrowRight, Boxes, MessageCircle, PackageCheck, ShieldCheck, Truck } from 'lucide-react'
import { Link } from 'react-router-dom'
import ProductCard from '../components/ProductCard'
import { useCatalog } from '../context/CatalogContext'
import { fallbackProducts } from '../data/products'
import { whatsappUrl } from '../lib/whatsapp'

const categoryVisuals = [
  ['Cash Bags', 'Secure · Durable · Reliable'],
  ['Luggage Bags', 'Travel ready · Built strong'],
  ['Jute Bags', 'Natural · Reusable · Brandable'],
  ['Zip Bags', 'Simple · Versatile · Practical'],
  ['Purses', 'Stylish · Functional · Wholesale'],
]

export default function Home() {
  const { products, config, loading, usingFallback } = useCatalog()
  const featured = products.slice(0, 4)
  const visualProducts = products.length ? products : (usingFallback ? fallbackProducts : [])
  const enquiryHref = whatsappUrl(
    config.whatsappNumber,
    'Hello, I would like to enquire about your wholesale bag range.',
  )

  return (
    <>
      <section className="hero">
        <div className="container hero-grid">
          <div className="hero-copy">
            <span className="kicker">Wholesale bags · Pan India supply</span>
            <h1>Quality bags for <em>growing businesses.</em></h1>
            <p>Reliable wholesale supply across cash bags, luggage, jute bags, zip bags and purses — with practical pricing and clear stock information.</p>
            <div className="hero-actions">
              <Link className="btn btn-primary btn-large" to="/products">
                View catalogue <ArrowRight size={18} />
              </Link>
              <a className="btn btn-secondary btn-large" href={enquiryHref}>
                <MessageCircle size={18} />
                Wholesale enquiry
              </a>
            </div>
            <div className="hero-mini-stats">
              <div><strong>5</strong><span>Product categories</span></div>
              <div><strong>Bulk</strong><span>Wholesale focused</span></div>
              <div><strong>Pan India</strong><span>Supply network</span></div>
            </div>
          </div>

          <div className="hero-showcase" aria-label="Featured wholesale bags">
            <div className="hero-card hero-card-main">
              {visualProducts[2]?.image
                ? <img src={visualProducts[2].image} alt="Jute wholesale bag" />
                : <div className="product-placeholder">Real product image will appear here</div>}
              <span>Better materials.<br />Brighter business.</span>
            </div>
            <div className="hero-card hero-card-top">
              {visualProducts[1]?.image
                ? <img src={visualProducts[1].image} alt="Travel bag" />
                : <div className="product-placeholder">Wholesale catalogue</div>}
            </div>
            <div className="hero-card hero-card-bottom">
              {visualProducts[4]?.image
                ? <img src={visualProducts[4].image} alt="Ladies purse" />
                : <div className="product-placeholder">Wholesale catalogue</div>}
            </div>
            <span className="script-note">More than bags — business carried forward.</span>
          </div>
        </div>
      </section>

      <section className="trust-strip">
        <div className="container trust-grid">
          <div><ShieldCheck /><span><strong>Trusted quality</strong><small>Built for repeat orders</small></span></div>
          <div><Boxes /><span><strong>Bulk supply</strong><small>Wholesale quantities</small></span></div>
          <div><PackageCheck /><span><strong>Ready stock</strong><small>Clear availability</small></span></div>
          <div><Truck /><span><strong>Pan India</strong><small>Reliable dispatch</small></span></div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-heading">
            <div>
              <span className="kicker">Browse by need</span>
              <h2>Shop by category</h2>
              <p>Five focused wholesale categories, kept simple and easy to browse.</p>
            </div>
            <Link className="text-link" to="/products">View all products <ArrowRight size={16} /></Link>
          </div>
          <div className="category-grid">
            {categoryVisuals.map(([name, subtitle], index) => {
              const sample = products.find((item) => item.category === name) ?? (usingFallback ? fallbackProducts[index] : undefined)
              return (
                <Link key={name} to={`/products?category=${encodeURIComponent(name)}`} className="category-card">
                  <div className="category-image">
                    {sample?.image
                      ? <img src={sample.image} alt="" loading="lazy" />
                      : <div className="product-placeholder">{name}</div>}
                  </div>
                  <strong>{name}</strong>
                  <span>{subtitle}</span>
                  <i><ArrowRight size={15} /></i>
                </Link>
              )
            })}
          </div>
        </div>
      </section>

      <section className="section section-soft">
        <div className="container">
          <div className="section-heading">
            <div>
              <span className="kicker">Popular wholesale choices</span>
              <h2>Featured products</h2>
              {usingFallback && <p>Demo catalogue shown until the server catalogue is published.</p>}
            </div>
            <Link className="text-link" to="/products">Full catalogue <ArrowRight size={16} /></Link>
          </div>

          {loading ? (
            <div className="empty-state"><p>Loading catalogue…</p></div>
          ) : featured.length > 0 ? (
            <div className="product-grid">
              {featured.map((product) => <ProductCard key={product.id} product={product} />)}
            </div>
          ) : (
            <div className="empty-state">
              <h2>Catalogue is being prepared</h2>
              <p>Products will appear here as soon as they are published from the private admin panel.</p>
              <Link className="btn btn-primary" to="/contact">Send a wholesale enquiry</Link>
            </div>
          )}
        </div>
      </section>

      <section className="section">
        <div className="container story-panel">
          <div className="story-image">
            {visualProducts[0]?.image
              ? <img src={visualProducts[0].image} alt="Wholesale bag supply" loading="lazy" />
              : <div className="product-placeholder">Real New Chandra Bags product photography will appear here.</div>}
          </div>
          <div className="story-copy">
            <span className="kicker">Built on trust</span>
            <h2>Simple wholesale. Clear information. Better business.</h2>
            <p>{config.brandName || 'New Chandra Bags'} is designed around how wholesale buyers actually shop: they need the material, price, minimum order quantity, current stock and restock time without digging through clutter.</p>
            <div className="story-stats">
              <div><strong>5+</strong><span>Core categories</span></div>
              <div><strong>100%</strong><span>Wholesale focused</span></div>
              <div><strong>Fast</strong><span>WhatsApp enquiry</span></div>
            </div>
            <Link className="btn btn-primary" to="/about">Our story <ArrowRight size={17} /></Link>
          </div>
        </div>
      </section>

      <section className="section cta-section">
        <div className="container cta-panel">
          <div>
            <span className="kicker kicker-light">Wholesale orders welcome</span>
            <h2>Need pricing for a larger quantity?</h2>
            <p>Send the product and quantity directly on WhatsApp. No cart, no checkout, no unnecessary steps.</p>
          </div>
          <a className="btn btn-light btn-large" href={enquiryHref}>
            <MessageCircle size={18} />
            Start an enquiry
          </a>
        </div>
      </section>
    </>
  )
}
