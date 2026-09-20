export type Product = {
  id: string
  slug: string
  name: string
  category: 'Cash Bags' | 'Luggage Bags' | 'Jute Bags' | 'Zip Bags' | 'Purses'
  material: string
  price: number
  moq: number
  stock: number
  restockDays?: number
  image: string
  accent: string
  description: string
  size: string
  features: string[]
}

export const categories = ['All', 'Cash Bags', 'Luggage Bags', 'Jute Bags', 'Zip Bags', 'Purses'] as const

export const products: Product[] = [
  {
    id: 'SB-001',
    slug: 'classic-cash-bag',
    name: 'Classic Cash Bag',
    category: 'Cash Bags',
    material: 'Heavy-duty polyester',
    price: 120,
    moq: 50,
    stock: 500,
    image: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=85',
    accent: '#991b1b',
    description: 'A secure, structured everyday cash bag designed for wholesale use in retail, offices and institutions.',
    size: '14 × 10 × 5 in',
    features: ['Double zipper', 'Reinforced handles', 'Two compartments', 'Custom logo available'],
  },
  {
    id: 'SB-102',
    slug: 'premium-travel-duffle',
    name: 'Premium Travel Duffle',
    category: 'Luggage Bags',
    material: 'Premium nylon',
    price: 350,
    moq: 30,
    stock: 120,
    image: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=85',
    accent: '#172554',
    description: 'A dependable travel duffle with reinforced construction, roomy storage and a clean business-ready finish.',
    size: '20 × 11 × 10 in',
    features: ['Large main compartment', 'Shoulder strap', 'Strong base', 'Bulk customisation'],
  },
  {
    id: 'SB-203',
    slug: 'eco-jute-tote',
    name: 'Eco Jute Tote',
    category: 'Jute Bags',
    material: 'Natural jute',
    price: 65,
    moq: 100,
    stock: 500,
    image: 'https://images.unsplash.com/photo-1594223274512-ad4803739b7c?auto=format&fit=crop&w=900&q=85',
    accent: '#8b6f47',
    description: 'A simple reusable jute tote for gifting, retail packaging, events and everyday wholesale supply.',
    size: '15 × 14 × 5 in',
    features: ['Reusable', 'Strong stitched handles', 'Logo printing', 'Eco-friendly material'],
  },
  {
    id: 'SB-304',
    slug: 'designer-zip-pouch',
    name: 'Designer Zip Pouch',
    category: 'Zip Bags',
    material: 'Canvas fabric',
    price: 45,
    moq: 100,
    stock: 0,
    restockDays: 7,
    image: 'https://images.unsplash.com/photo-1594223274512-ad4803739b7c?auto=format&fit=crop&w=900&q=85',
    accent: '#be123c',
    description: 'Compact, practical and easy to brand, this zip pouch works well for gifting, storage and promotional orders.',
    size: '10 × 7 in',
    features: ['Smooth zipper', 'Printed options', 'Lightweight', 'Custom colours'],
  },
  {
    id: 'SB-401',
    slug: 'ladies-everyday-purse',
    name: 'Ladies Everyday Purse',
    category: 'Purses',
    material: 'PU leather',
    price: 180,
    moq: 50,
    stock: 75,
    image: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=900&q=85',
    accent: '#9f1239',
    description: 'A polished everyday purse with a compact silhouette and practical storage for wholesale fashion supply.',
    size: '11 × 8 × 4 in',
    features: ['Soft lining', 'Secure closure', 'Multiple pockets', 'Wholesale colour options'],
  },
  {
    id: 'SB-205',
    slug: 'printed-jute-shopper',
    name: 'Printed Jute Shopper',
    category: 'Jute Bags',
    material: 'Laminated jute',
    price: 82,
    moq: 100,
    stock: 340,
    image: 'https://images.unsplash.com/photo-1594223274512-ad4803739b7c?auto=format&fit=crop&w=900&q=85',
    accent: '#7c5c3e',
    description: 'A stronger laminated jute shopper made for branded retail packaging, corporate gifting and repeat use.',
    size: '16 × 15 × 6 in',
    features: ['Laminated interior', 'Print-ready surface', 'Wide gusset', 'Strong handles'],
  },
]

export const featuredProducts = products.slice(0, 4)
export const whatsappNumber = '919876543210'