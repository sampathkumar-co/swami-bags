import type { ProductCategory, PublicConfig } from '../types/catalog'

export const categories = ['All', 'Cash Bags', 'Luggage Bags', 'Jute Bags', 'Zip Bags', 'Purses'] as const
export const productCategories: ProductCategory[] = ['Cash Bags', 'Luggage Bags', 'Jute Bags', 'Zip Bags', 'Purses']

export const defaultPublicConfig: PublicConfig = {
  brandName: 'New Chandra Bags',
  whatsappNumber: '',
  businessPhone: '',
  businessEmail: '',
  businessAddress: '',
  publicBaseUrl: '',
  logoUrl: '',
}