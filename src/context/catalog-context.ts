import { createContext, useContext } from 'react'
import type { CatalogProduct, PublicConfig } from '../types/catalog'

export type CatalogContextValue = {
  products: CatalogProduct[]
  config: PublicConfig
  loading: boolean
  refresh: () => Promise<void>
}

export const CatalogContext = createContext<CatalogContextValue | null>(null)

export function useCatalog() {
  const value = useContext(CatalogContext)
  if (!value) throw new Error('useCatalog must be used inside CatalogProvider')
  return value
}
