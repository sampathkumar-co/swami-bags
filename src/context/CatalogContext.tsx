import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { defaultPublicConfig, fallbackProducts } from '../data/products'
import type { CatalogProduct, CatalogSnapshot, PublicConfig } from '../types/catalog'

type CatalogContextValue = {
  products: CatalogProduct[]
  config: PublicConfig
  loading: boolean
  usingFallback: boolean
  refresh: () => Promise<void>
}

const CatalogContext = createContext<CatalogContextValue | null>(null)

export function CatalogProvider({ children }: { children: ReactNode }) {
  const [products, setProducts] = useState<CatalogProduct[]>([])
  const [config, setConfig] = useState<PublicConfig>(defaultPublicConfig)
  const [loading, setLoading] = useState(true)
  const [usingFallback, setUsingFallback] = useState(false)

  const refresh = useCallback(async () => {
    const nonce = Date.now()
    const [catalogResult, configResult] = await Promise.allSettled([
      fetch(`/catalog/products.json?v=${nonce}`, { cache: 'no-store' }),
      fetch(`/catalog/config.json?v=${nonce}`, { cache: 'no-store' }),
    ])

    let catalogLoaded = false
    if (catalogResult.status === 'fulfilled' && catalogResult.value.ok) {
      const snapshot = await catalogResult.value.json() as CatalogSnapshot
      if (snapshot && Array.isArray(snapshot.products)) {
        setProducts(snapshot.products)
        setUsingFallback(false)
        catalogLoaded = true
      }
    }

    if (!catalogLoaded) {
      if (import.meta.env.DEV) {
        setProducts(fallbackProducts)
        setUsingFallback(true)
      } else {
        setProducts([])
        setUsingFallback(false)
      }
    }

    if (configResult.status === 'fulfilled' && configResult.value.ok) {
      const publicConfig = await configResult.value.json() as Partial<PublicConfig>
      setConfig({ ...defaultPublicConfig, ...publicConfig })
    }
    setLoading(false)
  }, [])

  useEffect(() => {
    void refresh()
    const onFocus = () => void refresh()
    window.addEventListener('focus', onFocus)
    return () => window.removeEventListener('focus', onFocus)
  }, [refresh])

  const value = useMemo(() => ({ products, config, loading, usingFallback, refresh }), [products, config, loading, usingFallback, refresh])
  return <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>
}

export function useCatalog() {
  const value = useContext(CatalogContext)
  if (!value) throw new Error('useCatalog must be used inside CatalogProvider')
  return value
}
