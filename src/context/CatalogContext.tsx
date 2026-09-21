import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { defaultPublicConfig } from '../data/products'
import type { CatalogProduct, CatalogSnapshot, PublicConfig } from '../types/catalog'
import { CatalogContext } from './catalog-context'

export function CatalogProvider({ children }: { children: ReactNode }) {
  const [products, setProducts] = useState<CatalogProduct[]>([])
  const [config, setConfig] = useState<PublicConfig>(defaultPublicConfig)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    const nonce = Date.now()
    const [catalogResult, configResult] = await Promise.allSettled([
      fetch(`/catalog/products.json?v=${nonce}`, { cache: 'no-store' }),
      fetch(`/catalog/config.json?v=${nonce}`, { cache: 'no-store' }),
    ])

    if (catalogResult.status === 'fulfilled' && catalogResult.value.ok) {
      const snapshot = await catalogResult.value.json() as CatalogSnapshot
      if (snapshot && Array.isArray(snapshot.products)) {
        setProducts(snapshot.products)
      }
    }
    // If the catalogue fetch fails, keep the last successful snapshot in memory.
    // On first load this naturally stays empty; we never fabricate demo inventory.

    if (configResult.status === 'fulfilled' && configResult.value.ok) {
      const publicConfig = await configResult.value.json() as Partial<PublicConfig>
      setConfig({ ...defaultPublicConfig, ...publicConfig })
    }
    setLoading(false)
  }, [])

  useEffect(() => {
    let active = true
    queueMicrotask(() => {
      if (active) void refresh()
    })
    const onFocus = () => void refresh()
    window.addEventListener('focus', onFocus)
    return () => {
      active = false
      window.removeEventListener('focus', onFocus)
    }
  }, [refresh])

  const value = useMemo(() => ({ products, config, loading, refresh }), [products, config, loading, refresh])
  return <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>
}
