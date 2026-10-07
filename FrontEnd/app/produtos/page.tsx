"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import { ProductList } from "@/components/product-list"
import { ProductFilters } from "@/components/product-filters"
import { ProductListSkeleton } from "@/components/product-list-skeleton"
import { ProductPagination } from "@/components/product-pagination"
import { ShoppingBag, Flame, Plus } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Categoria, PaginaProdutos, ProdutoWithPrices } from "@/lib/types"

const API_URL = "http://localhost:8080"
const PRODUTOS_POR_PAGINA = 24

export default function ProductsPage() {
  const [products, setProducts] = useState<ProdutoWithPrices[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [loading, setLoading] = useState(true)
  const [pagina, setPagina] = useState(0)
  const [totalPaginas, setTotalPaginas] = useState(0)
  const [totalProducts, setTotalProducts] = useState(0)
  const [filters, setFilters] = useState({
    search: "",
    category: "",
    sortBy: "",
  })

  // categorias reais do banco (as mesmas criadas pelo webscraping)
  useEffect(() => {
    fetch(`${API_URL}/categorias`)
      .then((res) => (res.ok ? res.json() : []))
      .then((data: Categoria[]) => setCategorias(data.filter((c) => c.quantidadeProdutos > 0)))
      .catch((error) => console.error("Erro ao buscar categorias:", error))
  }, [])

  // busca só a página atual; filtros e ordenação são feitos no backend
  useEffect(() => {
    const controller = new AbortController()

    const fetchProducts = async () => {
      setLoading(true)
      try {
        const params = new URLSearchParams({ pagina: String(pagina), tamanho: String(PRODUTOS_POR_PAGINA) })
        if (filters.search) params.set("busca", filters.search)
        if (filters.category) params.set("categoria", filters.category)
        if (filters.sortBy) params.set("ordenar", filters.sortBy)

        const response = await fetch(`${API_URL}/produtos/paginado?${params}`, { signal: controller.signal })
        if (!response.ok) throw new Error("Erro ao buscar produtos")
        const data: PaginaProdutos = await response.json()

        // o backend já devolve os preços do menor para o maior
        const produtosComMenorPreco = data.itens.map((produto) => ({
          ...produto,
          menorPreco: produto.precos.length > 0 ? produto.precos[0] : undefined,
        }))

        setProducts(produtosComMenorPreco)
        setTotalPaginas(data.totalPaginas)
        setTotalProducts(data.total)
        setLoading(false)
      } catch (error) {
        if (controller.signal.aborted) return // uma requisição mais nova substituiu esta
        console.error("Erro ao buscar produtos:", error)
        setProducts([])
        setTotalPaginas(0)
        setTotalProducts(0)
        setLoading(false)
      }
    }

    fetchProducts()
    return () => controller.abort()
  }, [pagina, filters])

  const filtersRef = useRef(filters)
  const handleFiltersChange = useCallback((newFilters: typeof filters) => {
    const atual = filtersRef.current
    // o componente de filtros também avisa ao montar; sem mudança real, não refaz a busca
    if (
      atual.search === newFilters.search &&
      atual.category === newFilters.category &&
      atual.sortBy === newFilters.sortBy
    ) return

    filtersRef.current = newFilters
    setFilters(newFilters)
    setPagina(0) // filtro novo sempre começa da primeira página
  }, [])

  const handlePageChange = (novaPagina: number) => {
    setPagina(novaPagina)
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
          <ProductFilters categories={categorias.map((c) => c.nome)} onFiltersChange={handleFiltersChange} />
        </div>

        <div className="animate-fade-in">{loading ? <ProductListSkeleton /> : <ProductList products={products} />}</div>

        {!loading && (
          <ProductPagination pagina={pagina} totalPaginas={totalPaginas} onPageChange={handlePageChange} />
        )}
      </div>
    </div>
  )
}
