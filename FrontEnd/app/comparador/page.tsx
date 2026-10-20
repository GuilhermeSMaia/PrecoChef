"use client"

import { Suspense, useState } from "react"
import { useSearchParams } from "next/navigation"
import { ProductComparison } from "@/components/product-comparison"
import { ProductSelector } from "@/components/product-selector"
import type { ProdutoWithPrices } from "@/lib/types"

function Comparador() {
  // o botão de olho do card abre /comparador?produto=<nome>
  const termoInicial = useSearchParams().get("produto") ?? ""
  const [selectedProduct, setSelectedProduct] = useState<ProdutoWithPrices | null>(null)

  return (
    <>
      <div className="mb-8">
        <ProductSelector onProductSelect={setSelectedProduct} termoInicial={termoInicial} />
      </div>

      <ProductComparison selectedProduct={selectedProduct} />
    </>
  )
}

export default function ComparadorPage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Comparador de Preços</h1>

      {/* useSearchParams exige Suspense no build do Next 14 */}
      <Suspense>
        <Comparador />
      </Suspense>
    </div>
  )
}
