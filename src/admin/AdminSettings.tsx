import { Check, LoaderCircle, Save, Settings } from 'lucide-react'
import { useEffect, useState } from 'react'
import { adminApi } from '../lib/adminApi'
import type { SiteSettings } from './types'

const emptySettings: SiteSettings = {
  brandName: 'Swami Bags',
  whatsappNumber: '',
  businessPhone: '',
  businessEmail: '',
  businessAddress: '',
  publicBaseUrl: '',
}

export default function AdminSettings() {
  const [form, setForm] = useState<SiteSettings>(emptySettings)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    adminApi.settings()
      .then((settings) => {
        if (active) setForm(settings)
      })
      .catch((caught) => {
        if (active) setError(caught instanceof Error ? caught.message : 'Unable to load business settings.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [])

  const update = <K extends keyof SiteSettings>(key: K, value: SiteSettings[K]) => {
    setForm((current) => ({ ...current, [key]: value }))
  }

  const save = async () => {
    setSaving(true)
    setError('')
    setMessage('')
    try {
      const saved = await adminApi.updateSettings(form)
      setForm(saved)
      setMessage('Business details saved and the public website configuration was refreshed.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to save business settings.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="admin-loading">Loading settings…</div>

  return (
    <div className="admin-page">
      <div className="admin-page-heading">
        <div>
          <span className="admin-kicker">Business setup</span>
          <h1>Website settings</h1>
          <p>Update the public contact and brand details here. No code change or rebuild is required.</p>
        </div>
      </div>

      {error && <div className="admin-error admin-error-wide">{error}</div>}
      {message && <div className="admin-success"><Check size={17} /> {message}</div>}

      <section className="admin-panel admin-settings-panel">
        <div className="admin-panel-head">
          <div><h2>Public business information</h2><p>These values are exported to the static public site after saving.</p></div>
          <Settings size={20} />
        </div>

        <div className="admin-form-grid">
          <label className="span-2">
            <span>Brand name *</span>
            <input required value={form.brandName} onChange={(e) => update('brandName', e.target.value)} placeholder="Swami Bags" />
          </label>

          <label>
            <span>WhatsApp number</span>
            <input value={form.whatsappNumber} onChange={(e) => update('whatsappNumber', e.target.value)} placeholder="919876543210" />
            <small>Include country code. Digits only is recommended.</small>
          </label>

          <label>
            <span>Business phone</span>
            <input value={form.businessPhone} onChange={(e) => update('businessPhone', e.target.value)} placeholder="+91 98765 43210" />
          </label>

          <label>
            <span>Business email</span>
            <input type="email" value={form.businessEmail} onChange={(e) => update('businessEmail', e.target.value)} placeholder="sales@example.com" />
          </label>

          <label>
            <span>Public website URL</span>
            <input value={form.publicBaseUrl} onChange={(e) => update('publicBaseUrl', e.target.value)} placeholder="https://example.com" />
          </label>

          <label className="span-2">
            <span>Business address</span>
            <textarea rows={4} value={form.businessAddress} onChange={(e) => update('businessAddress', e.target.value)} placeholder="Wholesale office / dispatch address" />
          </label>
        </div>

        <div className="admin-form-actions">
          <button className="btn btn-primary" type="button" onClick={() => void save()} disabled={saving}>
            {saving ? <LoaderCircle className="spin" size={17} /> : <Save size={17} />}
            {saving ? 'Saving…' : 'Save settings'}
          </button>
          <span className="admin-settings-note">Changing these values does not restart the website.</span>
        </div>
      </section>
    </div>
  )
}
