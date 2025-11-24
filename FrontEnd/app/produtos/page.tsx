
"use client"

import { useState, useEffect, useCallback } from "react"
import { ProductList } from "@/components/product-list"
import { ProductFilters } from "@/components/product-filters"
import { ProductListSkeleton } from "@/components/product-list-skeleton"
import { DataService } from "@/lib/data-service"
import { ShoppingBag, Flame, Plus } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { ProdutoWithPrices } from "@/lib/types"

export default function ProductsPage() {
  const [products, setProducts] = useState<ProdutoWithPrices[]>([])
  const [loading, setLoading] = useState(true)
  const [filters, setFilters] = useState({
    search: "",
    category: "",
    sortBy: "",
  })

   useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await fetch("http://localhost:8080/produtos/getAll")
        if (!response.ok) throw new Error("Erro ao buscar produtos")
        const data = await response.json()

        const produtosComMenorPreco = data.map((produto: ProdutoWithPrices) => {
        if (produto.precos.length > 0) {
          const menor = produto.precos.reduce((min, atual) =>
            atual.preco < min.preco ? atual : min
          )
          return { ...produto, menorPreco: menor }
        }
        return { ...produto, menorPreco: undefined }
      })

        setProducts(produtosComMenorPreco)
      } catch (error) {
        console.error("Erro ao buscar produtos:", error)
        setProducts([])
      } finally {
        setLoading(false)
      }
    }

    fetchProducts()
  }, [])

  
  
  const handleFiltersChange = useCallback((newFilters: typeof filters) => {
    setFilters(newFilters)
  }, [])

  const totalProducts = products.length

  return (
    <div className="min-h-screen gradient-bg">
      <div className="container mx-auto px-4 py-8">
        <div className="mb-8 animate-fade-in">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
            <div>
              <h1 className="text-3xl md:text-4xl font-bold mb-2">Produtos e Preços</h1>
              <p className="text-xl text-muted-foreground">
                Compare preços entre mercados e encontre as melhores ofertas
              </p>
            </div>
            <Link href="/admin">
              <Button className="gap-2 hover-lift">
                <Plus className="h-4 w-4" />
                Cadastrar Produto
              </Button>
            </Link>
          </div>

          {/* Stats Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
            <Card className="border-0 shadow-md hover-lift orange-gradient">
              <CardContent className="p-6 flex items-center gap-4">
                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center">
                  <ShoppingBag className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{totalProducts}</p>
                  <p className="text-sm text-muted-foreground">Produtos disponíveis</p>
                </div>
              </CardContent>
            </Card>

           
          </div>
        </div>

        <div className="mb-8 animate-slide-up">
          <ProductFilters onFiltersChange={handleFiltersChange} />
        </div>

        <div className="animate-fade-in">{loading ? <ProductListSkeleton /> : <ProductList products={products} />}</div>
      </div>
    </div>
  )
}
