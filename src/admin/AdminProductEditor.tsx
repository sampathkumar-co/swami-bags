import { ArrowLeft, Check, ImagePlus, LoaderCircle, Save, Sparkles, Trash2, Upload } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { productCategories } from '../data/products'
import { adminApi } from '../lib/adminApi'
import type { AdminProduct, AiGeneration, ProductPayload } from './types'

type FormState = {
  id: string
  name: string
  category: ProductPayload['category']
  material: string
  price: string
  priceUnit: string
  moq: string
  stock: string
  restockDays: string
  size: string
  description: string
  features: string
}

const emptyForm: FormState = {
  id: '',
  name: '',
  category: 'Cash Bags',
  material: '',
  price: '0',
  priceUnit: 'piece',
  moq: '50',
  stock: '0',
  restockDays: '',
  size: '',
  description: '',
  features: '',
}

export default function AdminProductEditor() {
  const { id } = useParams()
  const isNew = !id
  const navigate = useNavigate()
  const [form, setForm] = useState<FormState>(emptyForm)
  const [product, setProduct] = useState<AdminProduct | null>(null)
  const [loading, setLoading] = useState(!isNew)
  const [busy, setBusy] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [aiConfigured, setAiConfigured] = useState(false)
  const [generationHistory, setGenerationHistory] = useState<AiGeneration[]>([])

  const load = useCallback(async () => {
    if (!id) return
    setLoading(true)
    try {
      const [loaded, history] = await Promise.all([adminApi.product(id), adminApi.generations(id)])
      setProduct(loaded)
      setGenerationHistory(history)
      setForm({
        id: loaded.id,
        name: loaded.name,
        category: loaded.category,
        material: loaded.material,
        price: String(loaded.price),
        priceUnit: loaded.priceUnit || 'piece',
        moq: String(loaded.moq),
        stock: String(loaded.stock),
        restockDays: loaded.restockDays == null ? '' : String(loaded.restockDays),
        size: loaded.size || '',
        description: loaded.description || '',
        features: loaded.features.join('\n'),
      })
      setError('')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to load product.')
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { void load() }, [load])

  useEffect(() => {
    let active = true
    adminApi.dashboard()
      .then((dashboard) => {
        if (active) setAiConfigured(dashboard.aiConfigured)
      })
      .catch(() => undefined)
    return () => { active = false }
  }, [])

  const payload = useMemo<ProductPayload>(() => ({
    id: isNew && form.id.trim() ? form.id.trim() : undefined,
    name: form.name.trim(),
    category: form.category,
    material: form.material.trim(),
    price: Math.max(0, Number(form.price) || 0),
    priceUnit: form.priceUnit.trim() || 'piece',
    moq: Math.max(1, Number(form.moq) || 1),
    stock: Math.max(0, Number(form.stock) || 0),
    restockDays: form.restockDays.trim() ? Math.max(0, Number(form.restockDays) || 0) : null,
    size: form.size.trim(),
    description: form.description.trim(),
    features: form.features.split('\n').map((item) => item.trim()).filter(Boolean),
  }), [form, isNew])

  const update = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((current) => ({ ...current, [key]: value }))

  const save = async (event?: FormEvent) => {
    event?.preventDefault()
    setBusy('save')
    setError('')
    setMessage('')
    try {
      if (!payload.name || !payload.material) throw new Error('Product name and material are required.')
      if (isNew) {
        const created = await adminApi.createProduct(payload)
        navigate(`/admin/products/${encodeURIComponent(created.id)}`, { replace: true })
      } else if (id) {
        const updated = await adminApi.updateProduct(id, payload)
        setProduct(updated)
        setMessage('Product details saved.')
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to save product.')
    } finally {
      setBusy('')
    }
  }

  const upload = async (files: FileList | null) => {
    if (!id || !files?.length) return
    setBusy('upload')
    setError('')
    try {
      await adminApi.uploadImages(id, Array.from(files))
      await load()

      if (aiConfigured) {
        setBusy('ai')
        try {
          await adminApi.generateMarketing(id)
          await load()
          setMessage('Product photos uploaded and a fresh AI marketing image was generated. Review and approve it below.')
        } catch (caught) {
          setMessage('Product photos were saved successfully.')
          setError(caught instanceof Error
            ? `Photos saved, but automatic AI generation failed: ${caught.message}`
            : 'Photos saved, but automatic AI generation failed.')
        }
      } else {
        setMessage('Product photos uploaded. AI generation will become available after OPENAI_API_KEY is configured.')
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to upload photos.')
    } finally {
      setBusy('')
    }
  }

  const generate = async () => {
    if (!id) return
    setBusy('ai')
    setError('')
    setMessage('')
    try {
      await adminApi.generateMarketing(id)
      await load()
      setMessage('AI marketing image generated. Review it below, then approve it.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to generate marketing image.')
    } finally {
      setBusy('')
    }
  }

  const approve = async (imageId: string) => {
    if (!id) return
    setBusy(imageId)
    try {
      await adminApi.approveMarketing(id, imageId)
      await load()
      setMessage('Marketing image approved for the public catalogue.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to approve image.')
    } finally {
      setBusy('')
    }
  }

  const removeImage = async (imageId: string) => {
    if (!id || !window.confirm('Delete this image?')) return
    setBusy(imageId)
    try {
      await adminApi.deleteImage(id, imageId)
      await load()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to delete image.')
    } finally {
      setBusy('')
    }
  }

  const togglePublish = async () => {
    if (!id || !product) return
    setBusy('publish')
    try {
      const updated = await adminApi.publish(id, !product.published)
      setProduct(updated)
      setMessage(updated.published ? 'Product published to the public catalogue.' : 'Product moved back to draft.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to change publish status.')
    } finally {
      setBusy('')
    }
  }

  const deleteProduct = async () => {
    if (!id || !window.confirm('Delete this product and its stored images? This cannot be undone.')) return
    setBusy('delete')
    try {
      await adminApi.deleteProduct(id)
      navigate('/admin', { replace: true })
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to delete product.')
      setBusy('')
    }
  }

  if (loading) return <div className="admin-loading">Loading product…</div>

  const originals = product?.images.filter((image) => image.kind === 'ORIGINAL') ?? []
  const marketing = product?.images.filter((image) => image.kind === 'MARKETING') ?? []

  return (
    <div className="admin-page">
      <div className="admin-editor-heading">
        <div>
          <Link className="admin-back" to="/admin"><ArrowLeft size={15} /> Dashboard</Link>
          <span className="admin-kicker">{isNew ? 'New product' : form.id}</span>
          <h1>{isNew ? 'Add a product' : form.name}</h1>
          <p>{isNew ? 'Enter the commercial details first. Photos and AI generation become available after saving.' : 'Update details, manage real photos and approve the AI marketing image.'}</p>
        </div>
        {!isNew && product && (
          <button className={product.published ? 'admin-publish-button live' : 'admin-publish-button'} onClick={() => void togglePublish()} disabled={!!busy}>
            <span></span>{product.published ? 'Published' : 'Draft'}
          </button>
        )}
      </div>

      {error && <div className="admin-error admin-error-wide">{error}</div>}
      {message && <div className="admin-success"><Check size={17} /> {message}</div>}

      <form className="admin-editor-grid" onSubmit={save}>
        <section className="admin-panel admin-form-panel">
          <div className="admin-panel-head"><div><h2>Product details</h2><p>Information shown to wholesale buyers.</p></div></div>

          <div className="admin-form-grid">
            <label className="span-2"><span>Product name *</span><input required value={form.name} onChange={(e) => update('name', e.target.value)} placeholder="Premium Methi Bag" /></label>
            <label><span>Product code</span><input disabled={!isNew} value={form.id} onChange={(e) => update('id', e.target.value)} placeholder="Auto-generated if empty" /></label>
            <label>
              <span>Category *</span>
              <select value={form.category} onChange={(e) => update('category', e.target.value as FormState['category'])}>
                {productCategories.map((category) => <option key={category}>{category}</option>)}
              </select>
            </label>
            <label className="span-2"><span>Material *</span><input required value={form.material} onChange={(e) => update('material', e.target.value)} placeholder="Methi + 3D fabric" /></label>
            <label><span>Wholesale price ₹</span><input type="number" min="0" step="0.01" value={form.price} onChange={(e) => update('price', e.target.value)} /></label>
            <label><span>Price unit</span><input value={form.priceUnit} onChange={(e) => update('priceUnit', e.target.value)} placeholder="piece" /></label>
            <label><span>MOQ</span><input type="number" min="1" value={form.moq} onChange={(e) => update('moq', e.target.value)} /></label>
            <label><span>Available quantity</span><input type="number" min="0" value={form.stock} onChange={(e) => update('stock', e.target.value)} /></label>
            <label><span>Restock days</span><input type="number" min="0" value={form.restockDays} onChange={(e) => update('restockDays', e.target.value)} placeholder="Only needed when stock is 0" /></label>
            <label><span>Size</span><input value={form.size} onChange={(e) => update('size', e.target.value)} placeholder="14 × 10 × 5 in" /></label>
            <label className="span-2"><span>Description</span><textarea rows={4} value={form.description} onChange={(e) => update('description', e.target.value)} placeholder="Short wholesale-focused product description." /></label>
            <label className="span-2"><span>Features — one per line</span><textarea rows={5} value={form.features} onChange={(e) => update('features', e.target.value)} placeholder={'Double zipper\nExtra storage\nStrong handles\nCustom printing'} /></label>
          </div>

          <div className="admin-form-actions">
            <button className="btn btn-primary" type="submit" disabled={!!busy}>
              {busy === 'save' ? <LoaderCircle className="spin" size={17} /> : <Save size={17} />}
              {isNew ? 'Save & continue' : 'Save changes'}
            </button>
            {!isNew && <button className="admin-danger-link" type="button" onClick={() => void deleteProduct()} disabled={!!busy}><Trash2 size={15} /> Delete product</button>}
          </div>
        </section>

        {!isNew && id && (
          <div className="admin-editor-side">
            <section className="admin-panel">
              <div className="admin-panel-head"><div><h2>Real product photos</h2><p>Upload up to 6 JPEG, PNG or WebP reference photos. When AI is configured, a marketing draft is generated automatically after upload.</p></div></div>
              <label className="admin-upload-zone">
                {busy === 'upload' ? <LoaderCircle className="spin" /> : <Upload />}
                <strong>{busy === 'upload' ? 'Uploading…' : 'Upload product photos'}</strong>
                <span>Good lighting and multiple angles improve AI fidelity.</span>
                <input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={(event) => void upload(event.target.files)} disabled={!!busy} />
              </label>

              <div className="admin-image-grid">
                {originals.map((image) => (
                  <div className="admin-image-card" key={image.imageId}>
                    <img src={image.publicUrl} alt="" />
                    <div><span>Original</span><button type="button" onClick={() => void removeImage(image.imageId)} aria-label="Delete original image"><Trash2 size={14} /></button></div>
                  </div>
                ))}
              </div>
              {originals.length === 0 && <div className="admin-inline-empty"><ImagePlus size={20} /> Add a real photo before generating AI artwork.</div>}
            </section>

            <section className="admin-panel">
              <div className="admin-panel-head">
                <div><h2>AI marketing image</h2><p>Product fidelity first; verified text is overlaid by the server.</p></div>
                <Sparkles size={20} />
              </div>
              <button className="btn btn-primary btn-full" type="button" onClick={() => void generate()} disabled={!!busy || originals.length === 0}>
                {busy === 'ai' ? <LoaderCircle className="spin" size={17} /> : <Sparkles size={17} />}
                {busy === 'ai' ? 'Generating marketing image…' : marketing.length ? 'Regenerate marketing image' : 'Generate marketing image'}
              </button>
              {busy === 'ai' && (
                <div className="admin-ai-progress">
                  <LoaderCircle className="spin" size={16} />
                  <span>Creating a faithful product visual. This can take a couple of minutes; keep this page open.</span>
                </div>
              )}

              <div className="admin-marketing-list">
                {marketing.map((image) => (
                  <div className={image.approved ? 'admin-marketing-card approved' : 'admin-marketing-card'} key={image.imageId}>
                    <img src={image.publicUrl} alt="Generated marketing visual" />
                    <div className="admin-marketing-actions">
                      <span>{image.approved ? <><Check size={14} /> Approved</> : 'Review before publishing'}</span>
                      <div>
                        {!image.approved && <button type="button" onClick={() => void approve(image.imageId)} disabled={!!busy}>Approve</button>}
                        <button type="button" onClick={() => void removeImage(image.imageId)} disabled={!!busy}><Trash2 size={14} /></button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {generationHistory.length > 0 && (
                <div className="admin-generation-history">
                  <h3>Recent AI attempts</h3>
                  {generationHistory.slice(0, 5).map((generation) => (
                    <div key={generation.id} className="admin-generation-row">
                      <span className={`admin-generation-status ${generation.status.toLowerCase()}`}>{generation.status}</span>
                      <div>
                        <strong>{generation.model}</strong>
                        <small>{new Date(generation.createdAt).toLocaleString()}</small>
                        {generation.errorMessage && <p>{generation.errorMessage}</p>}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}
      </form>
    </div>
  )
}
