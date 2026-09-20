import { MessageCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { Product } from '../data/products'
import { whatsappNumber } from '../data/products'

export default function ProductCard({ product }: { product: Product }) {
  const available = product.stock > 0
  const message = `Hello, I'm interested in ${product.name} (${product.id}). MOQ: ${product.moq} pieces. Please share current wholesale pricing and availability.`
  const whatsappHref = `https://wa.me/${whatsappNumber}?text=${encodeURIComponent(message)}`

  return (
    <article className="product-card">
      <Link to={`/products/${product.slug}`} className="product-image-wrap">
        <img src={product.image} alt={product.name} className="product-image" loading="lazy" />
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
          <span>/ piece</span>
        </div>
        <div className="product-meta">
          <span>MOQ: {product.moq} pcs</span>
          <span>{available ? `${product.stock} available` : 'Currently unavailable'}</span>
        </div>
        <a className="btn btn-whatsapp btn-full" href={whatsappHref} target="_blank" rel="noreferrer">
          <MessageCircle size={16} />
          Enquire on WhatsApp
        </a>
      </div>
    </article>
  )
}