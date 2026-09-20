import type { AdminImage, AdminProduct, AiGeneration, Dashboard, ProductPayload, SiteSettings } from '../admin/types'

let csrfToken = ''

async function readError(response: Response) {
  try {
    const body = await response.json() as { message?: string }
    return body.message || `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}

async function ensureCsrf(force = false) {
  if (csrfToken && !force) return csrfToken
  const response = await fetch('/api/admin/auth/csrf', { credentials: 'include', cache: 'no-store' })
  if (!response.ok) throw new Error(await readError(response))
  const body = await response.json() as { token: string }
  csrfToken = body.token
  return csrfToken
}

async function request<T>(path: string, init: RequestInit = {}, retryCsrf = true): Promise<T> {
  const method = (init.method || 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    headers.set('X-XSRF-TOKEN', await ensureCsrf())
  }
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, { ...init, headers, credentials: 'include', cache: 'no-store' })
  if (response.status === 403 && retryCsrf && !['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    await ensureCsrf(true)
    return request<T>(path, init, false)
  }
  if (!response.ok) throw new Error(await readError(response))
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const adminApi = {
  async login(username: string, password: string) {
    const body = new URLSearchParams({ username, password })
    return request<{ authenticated: boolean; username: string }>('/api/admin/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    })
  },

  me() {
    return request<{ authenticated: boolean; username: string }>('/api/admin/auth/me')
  },

  logout() {
    return request<{ authenticated: boolean }>('/api/admin/auth/logout', { method: 'POST' })
  },

  dashboard() {
    return request<Dashboard>('/api/admin/dashboard')
  },

  products() {
    return request<AdminProduct[]>('/api/admin/products')
  },

  product(id: string) {
    return request<AdminProduct>(`/api/admin/products/${encodeURIComponent(id)}`)
  },

  createProduct(payload: ProductPayload) {
    return request<AdminProduct>('/api/admin/products', { method: 'POST', body: JSON.stringify(payload) })
  },

  updateProduct(id: string, payload: ProductPayload) {
    return request<AdminProduct>(`/api/admin/products/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  publish(id: string, published: boolean) {
    return request<AdminProduct>(`/api/admin/products/${encodeURIComponent(id)}/publish`, {
      method: 'POST',
      body: JSON.stringify({ published }),
    })
  },

  async uploadImages(id: string, files: File[]) {
    const form = new FormData()
    files.forEach((file) => form.append('files', file))
    return request<AdminImage[]>(`/api/admin/products/${encodeURIComponent(id)}/images`, {
      method: 'POST',
      body: form,
    })
  },

  generateMarketing(id: string) {
    return request<AdminImage>(`/api/admin/products/${encodeURIComponent(id)}/marketing/generate`, {
      method: 'POST',
    })
  },

  generations(productId: string) {
    return request<AiGeneration[]>(`/api/admin/products/generations?productId=${encodeURIComponent(productId)}`)
  },

  approveMarketing(id: string, imageId: string) {
    return request<AdminProduct>(
      `/api/admin/products/${encodeURIComponent(id)}/marketing/${encodeURIComponent(imageId)}/approve`,
      { method: 'POST' },
    )
  },

  deleteImage(id: string, imageId: string) {
    return request<void>(`/api/admin/products/${encodeURIComponent(id)}/images/${encodeURIComponent(imageId)}`, {
      method: 'DELETE',
    })
  },

  deleteProduct(id: string) {
    return request<void>(`/api/admin/products/${encodeURIComponent(id)}`, { method: 'DELETE' })
  },

  settings() {
    return request<SiteSettings>('/api/admin/settings')
  },

  updateSettings(payload: SiteSettings) {
    return request<SiteSettings>('/api/admin/settings', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  uploadLogo(file: File) {
    const form = new FormData()
    form.append('file', file)
    return request<SiteSettings>('/api/admin/settings/logo', {
      method: 'POST',
      body: form,
    })
  },

  deleteLogo() {
    return request<SiteSettings>('/api/admin/settings/logo', { method: 'DELETE' })
  },

  changePassword(currentPassword: string, newPassword: string) {
    return request<{ changed: boolean }>('/api/admin/auth/password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword }),
    })
  },
}
