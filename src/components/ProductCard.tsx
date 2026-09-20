import { MessageCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useCatalog } from '../context/CatalogContext'
import { whatsappUrl } from '../lib/whatsapp'
import type { CatalogProduct } from '../types/catalog'

export default function ProductCard({ product }: { product: CatalogProduct }) {
  const { config } = useCatalog()
  const available = product.stock > 0
  const message = [
    `Hello, I'm interested in ${product.name} (${product.id}).`,
    `MOQ: ${product.moq} pieces.`,
    'Please share current wholesale pricing and availability.',
  ].join('\n')
  const enquiryHref = whatsappUrl(config.whatsappNumber, message)

  return (
    <article className="product-card">
      <Link to={`/products/${product.slug}`} className="product-image-wrap">
        {product.image ? (
          <img src={product.image} alt={product.name} className="product-image" loading="lazy" />
        ) : (
          <div className="product-image product-placeholder">Image coming soon</div>
        )}
        <span className={available ? 'stock-badge in-stock' : 'stock-badge out-stock'}>
          {available ? 'In stock' : `Restock in ~${product.restockDays ?? 7} days`}
        </span>
      </Link>
      <div className="product-card-body">
        <div>
          <span className="eyebrow">{product.category}</span>
          <Link to={`/products/${product.slug}`} className="product-title">{product.name}</Link>
          <span className="product-code">{product.id} · {product.material}</span>
        </div>
        <div className="price-row">
          <strong>₹{product.price}</strong>
          <span>/ {product.priceUnit || 'piece'}</span>
        </div>
        <div className="product-meta">
          <span>MOQ: {product.moq} pcs</span>
          <span>{available ? `${product.stock} available` : 'Currently unavailable'}</span>
        </div>
        <a className="btn btn-whatsapp btn-full" href={enquiryHref}>
          <MessageCircle size={16} />
          Enquire on WhatsApp
        </a>
      </div>
    </article>
  )
}
