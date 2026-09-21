import { AlertTriangle, CheckCircle2, Image, LoaderCircle, Package, Pencil, Plus, Sparkles } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../lib/adminApi'
import type { AdminProduct, Dashboard } from './types'

export default function AdminDashboard() {
  const [stats, setStats] = useState<Dashboard | null>(null)
  const [products, setProducts] = useState<AdminProduct[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState('')

  const load = useCallback(async () => {
    try {
      const [dashboard, list] = await Promise.all([adminApi.dashboard(), adminApi.products()])
      setStats(dashboard)
      setProducts(list)
      setError('')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to load admin data.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    queueMicrotask(() => {
      if (active) void load()
    })
    return () => { active = false }
  }, [load])

  const togglePublish = async (product: AdminProduct) => {
    setBusyId(product.id)
    try {
      await adminApi.publish(product.id, !product.published)
      await load()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to update publish status.')
    } finally {
      setBusyId('')
    }
  }

  return (
    <div className="admin-page">
      <div className="admin-page-heading">
        <div><span className="admin-kicker">Overview</span><h1>Catalogue dashboard</h1><p>Manage products without touching code or rebuilding the public website.</p></div>
        <Link className="btn btn-primary" to="/admin/products/new"><Plus size={17} /> Add product</Link>
      </div>

      {error && <div className="admin-error admin-error-wide">{error}</div>}

      <div className="admin-stat-grid">
        <article><span><Package /></span><div><small>Products</small><strong>{stats?.products ?? '—'}</strong></div></article>
        <article><span><CheckCircle2 /></span><div><small>Published</small><strong>{stats?.published ?? '—'}</strong></div></article>
        <article><span><AlertTriangle /></span><div><small>Out of stock</small><strong>{stats?.outOfStock ?? '—'}</strong></div></article>
        <article><span><Image /></span><div><small>Images</small><strong>{stats?.images ?? '—'}</strong></div></article>
      </div>

      <div className={stats?.aiConfigured ? 'admin-ai-state ready' : 'admin-ai-state'}>
        <Sparkles size={18} />
        <div>
          <strong>{stats?.aiConfigured ? 'AI image generation ready' : 'AI key not configured yet'}</strong>
          <span>{stats?.aiConfigured ? 'New marketing visuals can be generated from real product photos.' : 'The website and product admin still work. Add OPENAI_API_KEY on the server later.'}</span>
        </div>
      </div>

      <section className="admin-panel">
        <div className="admin-panel-head">
          <div><h2>Products</h2><p>Published products are exported to the static public catalogue automatically.</p></div>
          <Link to="/admin/products/new" className="admin-text-link"><Plus size={15} /> New product</Link>
        </div>

        {loading ? (
          <div className="admin-empty">
            <LoaderCircle className="spin" />
            <p>Loading products…</p>
          </div>
        ) : products.length === 0 ? (
          <div className="admin-empty">
            <Package />
            <h3>No products yet</h3>
            <p>Add the first bag, upload its real photos, generate a marketing image and publish it.</p>
            <Link className="btn btn-primary" to="/admin/products/new">Add first product</Link>
          </div>
        ) : (
          <div className="admin-table-wrap">
            <table className="admin-table">
              <thead><tr><th>Product</th><th>Category</th><th>Price</th><th>Stock</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {products.map((product) => {
                  const preview = product.images.find((image) => image.kind === 'MARKETING' && image.approved)
                    ?? product.images.find((image) => image.kind === 'ORIGINAL')
                  return (
                    <tr key={product.id}>
                      <td>
                        <div className="admin-product-cell">
                          <div className="admin-product-thumb">{preview ? <img src={preview.publicUrl} alt="" /> : <span>SB</span>}</div>
                          <div><strong>{product.name}</strong><small>{product.id} · MOQ {product.moq}</small></div>
                        </div>
                      </td>
                      <td>{product.category}</td>
                      <td>₹{product.price} / {product.priceUnit}</td>
                      <td><span className={product.stock === 0 ? 'admin-stock zero' : 'admin-stock'}>{product.stock}</span></td>
                      <td>
                        <button
                          className={product.published ? 'admin-status published' : 'admin-status draft'}
                          disabled={busyId === product.id}
                          onClick={() => void togglePublish(product)}
                        >
                          {product.published ? 'Published' : 'Draft'}
                        </button>
                      </td>
                      <td><Link className="admin-icon-button" to={`/admin/products/${encodeURIComponent(product.id)}`} aria-label={`Edit ${product.name}`}><Pencil size={16} /></Link></td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
