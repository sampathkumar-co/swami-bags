import { Check, ImagePlus, KeyRound, LoaderCircle, Save, Settings, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useCatalog } from '../context/catalog-context'
import { adminApi } from '../lib/adminApi'
import type { SiteSettings } from './types'

const emptySettings: SiteSettings = {
  brandName: 'New Chandra Bags',
  whatsappNumber: '',
  businessPhone: '',
  businessEmail: '',
  businessAddress: '',
  publicBaseUrl: '',
  logoUrl: '',
}

export default function AdminSettings() {
  const { refresh: refreshPublicConfig } = useCatalog()
  const [form, setForm] = useState<SiteSettings>(emptySettings)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [logoBusy, setLogoBusy] = useState(false)
  const [passwordBusy, setPasswordBusy] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
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
      await refreshPublicConfig()
      setMessage('Business details saved and the public website configuration was refreshed.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to save business settings.')
    } finally {
      setSaving(false)
    }
  }

  const uploadLogo = async (file?: File) => {
    if (!file) return
    setLogoBusy(true)
    setError('')
    setMessage('')
    try {
      const saved = await adminApi.uploadLogo(file)
      setForm(saved)
      await refreshPublicConfig()
      setMessage('Business logo uploaded and published to the website.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to upload the logo.')
    } finally {
      setLogoBusy(false)
    }
  }

  const removeLogo = async () => {
    setLogoBusy(true)
    setError('')
    setMessage('')
    try {
      const saved = await adminApi.deleteLogo()
      setForm(saved)
      await refreshPublicConfig()
      setMessage('Business logo removed. The NC fallback mark is active.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to remove the logo.')
    } finally {
      setLogoBusy(false)
    }
  }

  const changePassword = async () => {
    setError('')
    setMessage('')
    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.')
      return
    }
    if (newPassword.length < 12) {
      setError('New password must be at least 12 characters.')
      return
    }
    setPasswordBusy(true)
    try {
      const result = await adminApi.changePassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setMessage(result.otherSessionsRevoked
        ? 'Admin password changed successfully. Other signed-in admin sessions were revoked.'
        : 'Admin password changed successfully.')
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to change the admin password.')
    } finally {
      setPasswordBusy(false)
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
          <div><h2>Business logo</h2><p>Upload the logo shown in the header, footer, favicon and social metadata.</p></div>
          <ImagePlus size={20} />
        </div>
        <div className="admin-logo-row">
          <div className="admin-logo-preview">
            {form.logoUrl ? <img src={form.logoUrl} alt="Current business logo" /> : <span>NC</span>}
          </div>
          <div className="admin-logo-copy">
            <strong>{form.logoUrl ? 'Custom logo active' : 'Using NC fallback mark'}</strong>
            <p>PNG or JPEG, up to 10 MB. A square or compact transparent logo works best.</p>
            <div className="admin-logo-actions">
              <label className="btn btn-primary">
                {logoBusy ? <LoaderCircle className="spin" size={17} /> : <ImagePlus size={17} />}
                {form.logoUrl ? 'Replace logo' : 'Upload logo'}
                <input
                  type="file"
                  accept="image/jpeg,image/png"
                  disabled={logoBusy}
                  onChange={(event) => {
                    const file = event.target.files?.[0]
                    event.target.value = ''
                    void uploadLogo(file)
                  }}
                />
              </label>
              {form.logoUrl && (
                <button className="btn admin-secondary-btn" type="button" onClick={() => void removeLogo()} disabled={logoBusy}>
                  <Trash2 size={16} /> Remove
                </button>
              )}
            </div>
          </div>
        </div>
      </section>

      <section className="admin-panel admin-settings-panel">
        <div className="admin-panel-head">
          <div><h2>Public business information</h2><p>These values are exported to the static public site after saving.</p></div>
          <Settings size={20} />
        </div>

        <div className="admin-form-grid">
          <label className="span-2">
            <span>Brand name *</span>
            <input required value={form.brandName} onChange={(e) => update('brandName', e.target.value)} placeholder="New Chandra Bags" />
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

      <section className="admin-panel admin-settings-panel">
        <div className="admin-panel-head">
          <div><h2>Admin password</h2><p>Change the catalogue admin password without editing server files.</p></div>
          <KeyRound size={20} />
        </div>
        <div className="admin-form-grid">
          <label className="span-2">
            <span>Current password</span>
            <input type="password" autoComplete="current-password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} />
          </label>
          <label>
            <span>New password</span>
            <input type="password" autoComplete="new-password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
            <small>Use at least 12 characters.</small>
          </label>
          <label>
            <span>Confirm new password</span>
            <input type="password" autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
          </label>
        </div>
        <div className="admin-form-actions">
          <button
            className="btn btn-primary"
            type="button"
            onClick={() => void changePassword()}
            disabled={passwordBusy || !currentPassword || !newPassword || !confirmPassword}
          >
            {passwordBusy ? <LoaderCircle className="spin" size={17} /> : <KeyRound size={17} />}
            {passwordBusy ? 'Changing…' : 'Change password'}
          </button>
          <span className="admin-settings-note">The password is stored as a BCrypt hash in the private SQLite database.</span>
        </div>
      </section>
    </div>
  )
}
