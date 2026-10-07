"use client"

import { useState, useEffect, useCallback } from "react"
import { ProductList } from "@/components/product-list"
import { ProductPagination } from "@/components/product-pagination"
import { ProductFilters } from "@/components/product-filters"
import { ProductListSkeleton } from "@/components/product-list-skeleton"
import { DataService } from "@/lib/data-service"
import { ShoppingBag, Flame, Plus } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { PaginaProdutos, ProdutoWithPrices } from "@/lib/types"

const PAGE_SIZE = 24

export default function ProductsPage() {
  const [products, setProducts] = useState<ProdutoWithPrices[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [totalProducts, setTotalProducts] = useState(0)
  const [filters, setFilters] = useState({
    search: "",
    category: "",
    sortBy: "",
  })

  // Busca só a página atual; filtros, ordenação e paginação são feitos no backend
  useEffect(() => {
    const controller = new AbortController()

    const fetchProducts = async () => {
      setLoading(true)
      try {
        const params = new URLSearchParams({ pagina: String(page - 1), tamanho: String(PAGE_SIZE) })
        if (filters.search) params.set("busca", filters.search)
        if (filters.category) params.set("categoria", filters.category)
        if (filters.sortBy) params.set("ordenar", filters.sortBy)

        const response = await fetch(`http://localhost:8080/produtos/paginado?${params}`, {
          signal: controller.signal,
        })
        if (!response.ok) throw new Error("Erro ao buscar produtos")
        const data: PaginaProdutos = await response.json()

        // o backend já devolve os preços do menor para o maior
        const produtosComMenorPreco = data.itens.map((produto) => ({
          ...produto,
          menorPreco: produto.precos.length > 0 ? produto.precos[0] : undefined,
        }))

        setProducts(produtosComMenorPreco)
        setTotalPages(Math.max(1, data.totalPaginas))
        setTotalProducts(data.total)
        setLoading(false)
      } catch (error) {
        if (controller.signal.aborted) return // uma requisição mais nova substituiu esta
        console.error("Erro ao buscar produtos:", error)
        setProducts([])
        setTotalPages(1)
        setTotalProducts(0)
        setLoading(false)
      }
    }

    fetchProducts()
    return () => controller.abort()
  }, [page, filters])

  const handleFiltersChange = useCallback((newFilters: typeof filters) => {
    // o componente de filtros também avisa ao montar; sem mudança real, mantém o mesmo objeto e não refaz a busca
    setFilters((atual) =>
      atual.search === newFilters.search &&
      atual.category === newFilters.category &&
      atual.sortBy === newFilters.sortBy
        ? atual
        : newFilters,
    )
    setPage(1)
  }, [])

  const currentPage = Math.min(page, totalPages)

  const handlePageChange = (newPage: number) => {
    setPage(newPage)
    window.scrollTo({ top: 0, behavior: "smooth" })
  }

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

        <div className="animate-fade-in">
          {loading ? (
            <ProductListSkeleton />
          ) : (
            <>
              <ProductList products={products} />
              <ProductPagination page={currentPage} totalPages={totalPages} onPageChange={handlePageChange} />
            </>
          )}
        </div>
      </div>
    </div>
  )
}
