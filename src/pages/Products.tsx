import { Search, SlidersHorizontal } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import ProductCard from '../components/ProductCard'
import { useCatalog } from '../context/catalog-context'
import { categories } from '../data/products'

export default function Products() {
  const { products, loading } = useCatalog()
  const [params, setParams] = useSearchParams()
  const requestedCategory = params.get('category') ?? 'All'
  const category = categories.includes(requestedCategory as typeof categories[number]) ? requestedCategory : 'All'
  const [query, setQuery] = useState('')

  const filtered = useMemo(() => {
    return products.filter((product) => {
      const matchesCategory = category === 'All' || product.category === category
      const haystack = [product.name, product.material, product.id, product.category].join(' ').toLowerCase()
      return matchesCategory && haystack.includes(query.toLowerCase())
    })
  }, [products, category, query])

  const changeCategory = (value: string) => {
    if (value === 'All') setParams({})
    else setParams({ category: value })
  }

  return (
    <section className="page-section catalogue-page">
      <div className="container">
        <div className="catalogue-header">
          <div>
            <span className="kicker">Wholesale catalogue</span>
            <h1>Our products</h1>
            <p>Browse the range with material, MOQ, wholesale price and availability shown clearly.</p>
          </div>
          <label className="search-box" aria-label="Search products">
            <Search size={18} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search products..." />
          </label>
        </div>

        <div className="filter-bar">
          <div className="filter-title"><SlidersHorizontal size={17} /> Categories</div>
          <div className="filter-pills">
            {categories.map((item) => (
              <button
                key={item}
                className={category === item ? 'filter-pill active' : 'filter-pill'}
                onClick={() => changeCategory(item)}
              >
                {item}
              </button>
            ))}
          </div>
        </div>

        <div className="catalogue-summary">
          <span><strong>{filtered.length}</strong> products shown</span>
          <span>Wholesale only · Direct enquiry</span>
        </div>

        {loading ? (
          <div className="empty-state"><p>Loading catalogue…</p></div>
        ) : filtered.length > 0 ? (
          <div className="product-grid product-grid-catalogue">
            {filtered.map((product) => <ProductCard key={product.id} product={product} />)}
          </div>
        ) : (
          <div className="empty-state">
            <h2>No products found</h2>
            <p>{products.length ? 'Try another category or clear your search.' : 'No products have been published yet.'}</p>
            {products.length > 0 && (
              <button className="btn btn-primary" onClick={() => { setQuery(''); changeCategory('All') }}>Clear filters</button>
            )}
          </div>
        )}
      </div>
    </section>
  )
}
