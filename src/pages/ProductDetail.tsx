import { ArrowLeft, Check, MessageCircle, PackageCheck, ShieldCheck, Truck } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import ProductCard from '../components/ProductCard'
import { products, whatsappNumber } from '../data/products'

export default function ProductDetail() {
  const { slug } = useParams()
  const product = products.find((item) => item.slug === slug)

  if (!product) {
    return (
      <section className="page-section">
        <div className="container empty-state">
          <h1>Product not found</h1>
          <p>The product may have moved or is no longer listed.</p>
          <Link className="btn btn-primary" to="/products">Back to catalogue</Link>
        </div>
      </section>
    )
  }

  const message = `Hello, I'm interested in ${product.name} (${product.id}). MOQ: ${product.moq} pieces. Please share current wholesale pricing and availability.`
  const whatsappHref = `https://wa.me/${whatsappNumber}?text=${encodeURIComponent(message)}`
  const related = products.filter((item) => item.category === product.category && item.id !== product.id).slice(0, 3)

  return (
    <>
      <section className="page-section product-detail-section">
        <div className="container">
          <Link className="back-link" to="/products"><ArrowLeft size={16} /> Back to catalogue</Link>
          <div className="product-detail-grid">
            <div className="detail-gallery">
              <div className="detail-main-image">
                <img src={product.image} alt={product.name} />
              </div>
              <div className="detail-thumbs">
                {[0, 1, 2, 3].map((index) => (
                  <button key={index} aria-label={`Product view ${index + 1}`}>
                    <img src={product.image} alt="" />
                  </button>
                ))}
              </div>
            </div>

            <div className="detail-content">
              <span className="kicker">{product.category}</span>
              <div className="detail-title-row">
                <div>
                  <h1>{product.name}</h1>
                  <span className="product-code">{product.id}</span>
                </div>
                <span className={product.stock > 0 ? 'stock-badge in-stock static-badge' : 'stock-badge out-stock static-badge'}>
                  {product.stock > 0 ? 'In stock' : 'Out of stock'}
                </span>
              </div>

              <div className="detail-price"><strong>₹{product.price}</strong><span>/ piece</span></div>
              <p className="detail-description">{product.description}</p>

              <div className="spec-table">
                <div><span>Material</span><strong>{product.material}</strong></div>
                <div><span>Minimum order</span><strong>{product.moq} pieces</strong></div>
                <div><span>Available quantity</span><strong>{product.stock > 0 ? `${product.stock} pieces` : 'Currently unavailable'}</strong></div>
                <div><span>Restock time</span><strong>{product.stock > 0 ? 'Regular supply' : `Approx. ${product.restockDays ?? 7} days`}</strong></div>
                <div><span>Size</span><strong>{product.size}</strong></div>
              </div>

              <a className="btn btn-whatsapp btn-large btn-full" href={whatsappHref} target="_blank" rel="noreferrer">
                <MessageCircle size={19} />
                Enquire on WhatsApp
              </a>
              <small className="helper-text">The message is pre-filled with this product code and MOQ.</small>
            </div>
          </div>
        </div>
      </section>

      <section className="detail-benefits">
        <div className="container trust-grid">
          <div><ShieldCheck /><span><strong>Durable material</strong><small>Wholesale-ready build</small></span></div>
          <div><PackageCheck /><span><strong>Bulk supply</strong><small>Clear MOQ & stock</small></span></div>
          <div><Truck /><span><strong>Pan India</strong><small>Reliable dispatch</small></span></div>
          <div><Check /><span><strong>Custom branding</strong><small>Available on request</small></span></div>
        </div>
      </section>

      <section className="section">
        <div className="container two-column-detail">
          <div>
            <span className="kicker">Product details</span>
            <h2>Made for practical everyday use.</h2>
            <p>{product.description}</p>
          </div>
          <div className="feature-list">
            {product.features.map((feature) => <div key={feature}><Check size={17} /> {feature}</div>)}
          </div>
        </div>
      </section>

      {related.length > 0 && (
        <section className="section section-soft">
          <div className="container">
            <div className="section-heading">
              <div><span className="kicker">More options</span><h2>Related products</h2></div>
            </div>
            <div className="product-grid related-grid">
              {related.map((item) => <ProductCard key={item.id} product={item} />)}
            </div>
          </div>
        </section>
      )}
    </>
  )
}