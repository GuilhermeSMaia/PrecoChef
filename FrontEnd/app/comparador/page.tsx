"use client"

import { useState } from "react"
import { ProductComparison } from "@/components/product-comparison"
import { ProductSelector } from "@/components/product-selector"
import type { Produto } from "@/lib/types"

export default function ComparadorPage() {
  const [selectedProduct, setSelectedProduct] = useState<Produto | null>(null)

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Comparador de Preços</h1>

      <div className="mb-8">
        <ProductSelector onProductSelect={setSelectedProduct} />
      </div>

      <ProductComparison selectedProduct={selectedProduct} />
    </div>
  )
}
