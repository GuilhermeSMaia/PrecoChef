"use client"

import { useState, useEffect } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Search } from "lucide-react"
import type { PaginaProdutos, ProdutoWithPrices } from "@/lib/types"

const LIMITE_SUGESTOES = 10

// busca no backend e já devolve os preços do menor para o maior
async function buscarProdutos(termo: string, signal?: AbortSignal): Promise<ProdutoWithPrices[]> {
  const params = new URLSearchParams({ pagina: "0", tamanho: String(LIMITE_SUGESTOES), busca: termo })
  const response = await fetch(`http://localhost:8080/produtos/paginado?${params}`, { signal })
  if (!response.ok) throw new Error("Erro ao buscar produtos")
  const data: PaginaProdutos = await response.json()

  return data.itens.map((produto) => {
    const precos = produto.precos
      .map((p) => ({ ...p, preco: Number(p.preco) }))
      .sort((a, b) => a.preco - b.preco)
    return { ...produto, precos, menorPreco: precos[0] }
  })
}

interface ProductSelectorProps {
  onProductSelect: (product: ProdutoWithPrices | null) => void
  // termo vindo da URL (/comparador?produto=...), selecionado automaticamente
  termoInicial?: string
}

export function ProductSelector({ onProductSelect, termoInicial = "" }: ProductSelectorProps) {
  const [searchTerm, setSearchTerm] = useState(termoInicial)
  const [suggestions, setSuggestions] = useState<ProdutoWithPrices[]>([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState<ProdutoWithPrices | null>(null)
  const [buscando, setBuscando] = useState(false)

  const selecionar = (product: ProdutoWithPrices) => {
    setSearchTerm(product.produtoname)
    setSelectedProduct(product)
    onProductSelect(product)
    setShowSuggestions(false)
  }

  const melhorCorrespondencia = (lista: ProdutoWithPrices[], termo: string) =>
    lista.find((p) => p.produtoname.toLowerCase() === termo.toLowerCase()) ?? lista[0]

  // seleciona automaticamente o produto vindo da URL
  useEffect(() => {
    if (!termoInicial) return
    const controller = new AbortController()
    buscarProdutos(termoInicial, controller.signal)
      .then((lista) => {
        const escolhido = melhorCorrespondencia(lista, termoInicial)
        if (escolhido) selecionar(escolhido)
      })
      .catch((error) => {
        if (!controller.signal.aborted) console.error("Erro ao buscar produto:", error)
      })
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [termoInicial])

  useEffect(() => {
    if (searchTerm.trim().length < 2) {
      setSuggestions([])
      return
    }

    const controller = new AbortController()
    const debounceTimer = setTimeout(async () => {
      setBuscando(true)
      try {
        setSuggestions(await buscarProdutos(searchTerm.trim(), controller.signal))
      } catch (error) {
        if (controller.signal.aborted) return // uma busca mais nova substituiu esta
        console.error("Erro ao buscar produtos:", error)
        setSuggestions([])
      } finally {
        if (!controller.signal.aborted) setBuscando(false)
      }
    }, 300)

    return () => {
      clearTimeout(debounceTimer)
      controller.abort()
    }
  }, [searchTerm])

  const handleSearch = () => {
    const escolhido = melhorCorrespondencia(suggestions, searchTerm.trim())
    if (escolhido) selecionar(escolhido)
    setShowSuggestions(false)
  }

  const semResultados =
    showSuggestions && searchTerm.trim().length >= 2 && !buscando && suggestions.length === 0 && !selectedProduct

  return (
    <div className="space-y-4">
      <div className="relative">
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Input
              placeholder="Digite o nome do produto para comparar..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value)
                setShowSuggestions(true)
                setSelectedProduct(null)
                if (!e.target.value) onProductSelect(null)
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleSearch()
              }}
              onFocus={() => setShowSuggestions(true)}
              className="pr-10"
            />
            <Button size="sm" variant="ghost" className="absolute right-0 top-0 h-full px-3" onClick={handleSearch}>
              <Search className="h-4 w-4" />
            </Button>
          </div>
          <Button onClick={handleSearch}>Comparar</Button>
        </div>

        {showSuggestions && searchTerm && suggestions.length > 0 && (
          <div className="absolute z-10 w-full mt-1 bg-background border rounded-md shadow-lg max-h-60 overflow-auto">
            {suggestions.map((product) => (
              <div
                key={product.id}
                className="px-4 py-3 hover:bg-accent cursor-pointer border-b last:border-b-0"
                onClick={() => selecionar(product)}
              >
                <div className="font-medium">{product.produtoname}</div>
                <div className="text-sm text-muted-foreground">
                  {product.categoria} • {product.medida} • {product.precos.length} mercado(s)
                </div>
              </div>
            ))}
          </div>
        )}

        {semResultados && (
          <div className="absolute z-10 w-full mt-1 bg-background border rounded-md shadow-lg px-4 py-3 text-sm text-muted-foreground">
            Nenhum produto encontrado para "{searchTerm}"
          </div>
        )}
      </div>

      {selectedProduct && (
        <div className="bg-muted/30 p-4 rounded-lg">
          <p className="text-sm text-muted-foreground">Comparando preços para:</p>
          <p className="font-medium text-lg">{selectedProduct.produtoname}</p>
          <p className="text-sm text-muted-foreground">
            {selectedProduct.categoria} • {selectedProduct.medida}
          </p>
        </div>
      )}
    </div>
  )
}
