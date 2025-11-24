
"use client"

import { useEffect, useState } from "react"
import { ProductCard } from "./product-card"
import { ProdutoWithPrices } from "@/lib/types"

interface ProductListProps {
  products: ProdutoWithPrices[]
}

export function ProductList({ products }: ProductListProps) {
  const [filteredProducts, setFilteredProducts] = useState<ProdutoWithPrices[]>([])

  useEffect(() => {
    setFilteredProducts(products)
  }, [products])

  if (filteredProducts.length === 0) {
    return (
      <div className="text-center py-8 bg-muted/30 rounded-lg">
        <p className="text-muted-foreground">Nenhum produto encontrado com os filtros selecionados.</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
      {filteredProducts.map((product) => (
        <ProductCard key={product.id} produto={product} />
      ))}
    </div>
  )
}
