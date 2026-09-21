import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { useCatalog } from '../context/catalog-context'
import { hasWhatsAppNumber } from '../lib/whatsapp'

function safeDecode(value: string) {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

function upsertMeta(selector: string, attribute: 'name' | 'property', key: string, content: string) {
  let element = document.head.querySelector<HTMLMetaElement>(selector)
  if (!element) {
    element = document.createElement('meta')
    element.setAttribute(attribute, key)
    document.head.appendChild(element)
  }
  element.setAttribute('content', content)
}

export default function DocumentMeta() {
  const location = useLocation()
  const { products, config } = useCatalog()

  useEffect(() => {
    const brand = config.brandName || 'New Chandra Bags'
    const path = location.pathname
    const productSlug = path.startsWith('/products/') ? safeDecode(path.slice('/products/'.length)) : ''
    const product = productSlug ? products.find((item) => item.slug === productSlug) : undefined
    const hasWhatsApp = hasWhatsAppNumber(config.whatsappNumber)

    let title = `${brand} | Wholesale Bag Catalogue`
    let description = `Wholesale cash bags, luggage bags, jute bags, zip bags and purses with clear MOQ, stock and ${hasWhatsApp ? 'direct WhatsApp' : 'direct'} enquiries.`
    let robots = 'index,follow,max-image-preview:large'

    if (path.startsWith('/admin')) {
      title = `${brand} | Catalogue Admin`
      description = 'Private catalogue administration.'
      robots = 'noindex,nofollow,noarchive'
    } else if (product) {
      title = `${product.name} | ${brand}`
      description = product.description || `${product.material} wholesale ${product.category.toLowerCase()} from ${brand}.`
    } else if (path === '/products') {
      title = `Wholesale Bag Catalogue | ${brand}`
      description = `Browse ${brand} wholesale cash bags, luggage bags, jute bags, zip bags and purses with MOQ and stock information.`
    } else if (path === '/about') {
      title = `About ${brand} | Wholesale Bags`
      description = `Learn about ${brand} and our wholesale bag supply approach.`
    } else if (path === '/contact') {
      title = `Wholesale Enquiry | ${brand}`
      description = `Contact ${brand} for wholesale bag pricing, quantities, availability and custom requirements.`
    } else if (path !== '/') {
      title = `Page Not Found | ${brand}`
      robots = 'noindex,follow'
    }

    document.title = title
    upsertMeta('meta[name="description"]', 'name', 'description', description)
    upsertMeta('meta[name="robots"]', 'name', 'robots', robots)
    upsertMeta('meta[property="og:title"]', 'property', 'og:title', title)
    upsertMeta('meta[property="og:description"]', 'property', 'og:description', description)

    const icon = document.head.querySelector<HTMLLinkElement>('link[rel="icon"]')
    if (icon) {
      icon.href = config.logoUrl || '/favicon.svg'
      icon.type = config.logoUrl
        ? (config.logoUrl.toLowerCase().endsWith('.png') ? 'image/png' : 'image/jpeg')
        : 'image/svg+xml'
    }

    const base = config.publicBaseUrl?.replace(/\/$/, '')
    if (config.logoUrl) {
      const socialImage = /^https?:\/\//i.test(config.logoUrl)
        ? config.logoUrl
        : (base && /^https?:\/\//i.test(base)
            ? base + (config.logoUrl.startsWith('/') ? '' : '/') + config.logoUrl
            : config.logoUrl)
      upsertMeta('meta[property="og:image"]', 'property', 'og:image', socialImage)
    } else {
      document.head.querySelector('meta[property="og:image"]')?.remove()
    }
    const canonicalHref = base && /^https?:\/\//i.test(base) ? `${base}${path === '/' ? '' : path}` : ''
    let canonical = document.head.querySelector<HTMLLinkElement>('link[rel="canonical"]')
    if (canonicalHref) {
      if (!canonical) {
        canonical = document.createElement('link')
        canonical.rel = 'canonical'
        document.head.appendChild(canonical)
      }
      canonical.href = canonicalHref
      upsertMeta('meta[property="og:url"]', 'property', 'og:url', canonicalHref)
    } else {
      canonical?.remove()
      document.head.querySelector('meta[property="og:url"]')?.remove()
    }
  }, [location.pathname, products, config])

  return null
}
