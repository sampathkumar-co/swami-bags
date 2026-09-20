import { ArrowLeft, Check, MessageCircle, Minus, PackageCheck, Plus, ShieldCheck, Truck } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import ProductCard from '../components/ProductCard'
import { useCatalog } from '../context/CatalogContext'
import { whatsappUrl } from '../lib/whatsapp'

export default function ProductDetail() {
  const { slug } = useParams()
  const { products, config, loading } = useCatalog()
  const product = products.find((item) => item.slug === slug)
  const [quantity, setQuantity] = useState(1)
  const [selectedImage, setSelectedImage] = useState('')

  useEffect(() => {
    if (product) {
      setQuantity(product.moq)
      setSelectedImage(product.image || product.images[0] || '')
    }
  }, [product])

  const enquiryHref = useMemo(() => {
    if (!product) return '/contact'
    const requested = Math.max(product.moq, quantity || product.moq)
    const message = [
      `Hello, I'm interested in ${product.name} (${product.id}).`,
      `Material: ${product.material}`,
      `Required quantity: ${requested} pieces`,
      product.price > 0 ? `Listed wholesale price: ₹${product.price} / ${product.priceUnit || 'piece'}` : 'Please share the current wholesale price.',
      'Please confirm current availability, final bulk pricing and dispatch time. Thank you.',
    ].join('\n')
    return whatsappUrl(config.whatsappNumber, message)
  }, [product, quantity, config.whatsappNumber])

  if (loading) {
    return <section className="page-section"><div className="container empty-state"><p>Loading product…</p></div></section>
  }

  if (!product) {
    return (
      <section className="page-section">
        <div className="container empty-state">
          <h1>Product not found</h1>
          <p>The product may have moved or is not currently published.</p>
          <Link className="btn btn-primary" to="/products">Back to catalogue</Link>
        </div>
      </section>
    )
  }

  const quantityStep = Math.max(1, Math.round(product.moq / 5))
  const related = products.filter((item) => item.category === product.category && item.id !== product.id).slice(0, 3)
  const gallery = Array.from(new Set([product.image, ...product.images].filter(Boolean)))

  return (
    <>
      <section className="page-section product-detail-section">
        <div className="container">
          <Link className="back-link" to="/products"><ArrowLeft size={16} /> Back to catalogue</Link>
          <div className="product-detail-grid">
            <div className="detail-gallery">
              <div className="detail-main-image">
                {selectedImage ? <img src={selectedImage} alt={product.name} /> : <div className="product-placeholder">Image coming soon</div>}
              </div>
              {gallery.length > 1 && (
                <div className="detail-thumbs">
                  {gallery.slice(0, 5).map((image, index) => (
                    <button
                      key={image}
                      className={selectedImage === image ? 'active' : ''}
                      aria-label={`Product view ${index + 1}`}
                      aria-pressed={selectedImage === image}
                      onClick={() => setSelectedImage(image)}
                    >
                      <img src={image} alt="" />
                    </button>
                  ))}
                </div>
              )}
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

              <div className="detail-price">
                {product.price > 0
                  ? <><strong>₹{product.price}</strong><span>/ {product.priceUnit || 'piece'}</span></>
                  : <strong>Price on enquiry</strong>}
              </div>
              <p className="detail-description">{product.description}</p>

              <div className="spec-table">
                <div><span>Material</span><strong>{product.material}</strong></div>
                <div><span>Minimum order</span><strong>{product.moq} pieces</strong></div>
                <div><span>Available quantity</span><strong>{product.stock > 0 ? `${product.stock} pieces` : 'Currently unavailable'}</strong></div>
                <div><span>Restock time</span><strong>{product.stock > 0 ? 'Regular supply' : `Approx. ${product.restockDays ?? 7} days`}</strong></div>
                <div><span>Size</span><strong>{product.size || 'On enquiry'}</strong></div>
              </div>

              <div className="quantity-enquiry">
                <div>
                  <span>Required quantity</span>
                  <small>MOQ {product.moq} pieces</small>
                </div>
                <div className="quantity-control">
                  <button aria-label="Decrease quantity" onClick={() => setQuantity((value) => Math.max(product.moq, value - quantityStep))}>
                    <Minus size={16} />
                  </button>
                  <input
                    type="number"
                    min={product.moq}
                    step={quantityStep}
                    value={quantity}
                    onChange={(event) => setQuantity(Math.max(product.moq, Number(event.target.value) || product.moq))}
                    aria-label="Required quantity"
                  />
                  <button aria-label="Increase quantity" onClick={() => setQuantity((value) => value + quantityStep)}>
                    <Plus size={16} />
                  </button>
                </div>
              </div>

              <a className="btn btn-whatsapp btn-large btn-full" href={enquiryHref}>
                <MessageCircle size={19} />
                Enquire on WhatsApp
              </a>
              <small className="helper-text">Product, material and requested quantity are added to the message automatically.</small>
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
            <div className="section-heading"><div><span className="kicker">More options</span><h2>Related products</h2></div></div>
            <div className="product-grid related-grid">{related.map((item) => <ProductCard key={item.id} product={item} />)}</div>
          </div>
        </section>
      )}
    </>
  )
}
