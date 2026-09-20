import type { ProductCategory } from '../types/catalog'

export type AdminImage = {
  imageId: string
  productId: string
  kind: 'ORIGINAL' | 'MARKETING'
  path: string
  publicUrl: string
  sortOrder: number
  approved: boolean
  createdAt: string
}

export type AdminProduct = {
  id: string
  slug: string
  name: string
  category: ProductCategory
  material: string
  price: number
  priceUnit: string
  moq: number
  stock: number
  restockDays?: number | null
  size: string
  description: string
  features: string[]
  published: boolean
  images: AdminImage[]
  createdAt: string
  updatedAt: string
}

export type ProductPayload = {
  id?: string
  slug?: string
  name: string
  category: ProductCategory
  material: string
  price: number
  priceUnit: string
  moq: number
  stock: number
  restockDays?: number | null
  size: string
  description: string
  features: string[]
}

export type Dashboard = {
  products: number
  published: number
  outOfStock: number
  images: number
  aiConfigured: boolean
}


export type SiteSettings = {
  brandName: string
  whatsappNumber: string
  businessPhone: string
  businessEmail: string
  businessAddress: string
  publicBaseUrl: string
  logoUrl: string
}

export type AiGeneration = {
  id: string
  productId: string
  model: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  resultImageId?: string | null
  errorMessage?: string | null
  createdAt: string
}
