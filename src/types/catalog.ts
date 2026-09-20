export type ProductCategory = 'Cash Bags' | 'Luggage Bags' | 'Jute Bags' | 'Zip Bags' | 'Purses'

export type CatalogProduct = {
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
  image: string
  images: string[]
}

export type CatalogSnapshot = {
  generatedAt: string
  products: CatalogProduct[]
}

export type PublicConfig = {
  brandName: string
  whatsappNumber: string
  businessPhone: string
  businessEmail: string
  businessAddress: string
  publicBaseUrl: string
  logoUrl: string
}
