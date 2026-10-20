"use client"

import { useEffect, useState } from "react"
import { ExternalLink, MapPin, Package, Search, Store } from "lucide-react"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { ProductPagination } from "@/components/product-pagination"
import { formatarData } from "@/lib/utils"
import type { MercadoComEnderecos, Pagina, ProdutoDoMercado } from "@/lib/types"

const TAMANHO_PAGINA = 10

interface MarketDetailsDialogProps {
  market: MercadoComEnderecos | null
  onClose: () => void
}

export function MarketDetailsDialog({ market, onClose }: MarketDetailsDialogProps) {
  const [produtos, setProdutos] = useState<ProdutoDoMercado[]>([])
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [erro, setErro] = useState(false)
  const [busca, setBusca] = useState("")
  const [buscaAplicada, setBuscaAplicada] = useState("")

  // ao trocar de mercado, volta para o início
  useEffect(() => {
    setPage(1)
    setBusca("")
    setBuscaAplicada("")
    setProdutos([])
    setTotal(0)
    setTotalPages(1)
  }, [market?.id])

  // espera o usuário parar de digitar antes de buscar
  useEffect(() => {
    const timer = setTimeout(() => {
      setBuscaAplicada(busca.trim())
      setPage(1)
    }, 300)
    return () => clearTimeout(timer)
  }, [busca])

  // busca só a página atual no backend
  useEffect(() => {
    if (!market) return
    const controller = new AbortController()

    const fetchProdutos = async () => {
      setLoading(true)
      setErro(false)
      try {
        const params = new URLSearchParams({ pagina: String(page - 1), tamanho: String(TAMANHO_PAGINA) })
        if (buscaAplicada) params.set("busca", buscaAplicada)
        const response = await fetch(`http://localhost:8080/mercados/${market.id}/produtos?${params}`, {
          signal: controller.signal,
        })
        if (!response.ok) throw new Error("Erro ao buscar produtos do mercado")
        const data: Pagina<ProdutoDoMercado> = await response.json()

        setProdutos(data.itens.map((p) => ({ ...p, preco: Number(p.preco) })))
        setTotal(data.total)
        setTotalPages(Math.max(1, data.totalPaginas))
        setLoading(false)
      } catch (error) {
        if (controller.signal.aborted) return // uma requisição mais nova substituiu esta
        console.error("Erro ao buscar produtos do mercado:", error)
        setProdutos([])
        setErro(true)
        setLoading(false)
      }
    }

    fetchProdutos()
    return () => controller.abort()
  }, [market, page, buscaAplicada])

  return (
    <Dialog open={market !== null} onOpenChange={(aberto) => !aberto && onClose()}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        {market && (
          <>
            <DialogHeader>
              <div className="flex items-center gap-4">
                <div className="h-14 w-14 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                  <MapPin className="h-7 w-7 text-primary" />
                </div>
                <div className="min-w-0 text-left">
                  <DialogTitle className="text-2xl md:text-3xl">{market.name}</DialogTitle>
                  <DialogDescription asChild>
                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1">
                      {market.url && (
                        <a
                          href={market.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1 text-primary hover:underline break-all"
                        >
                          {market.url}
                          <ExternalLink className="h-3.5 w-3.5 flex-shrink-0" />
                        </a>
                      )}
                      <span className="inline-flex items-center gap-1">
                        <Store className="h-3.5 w-3.5" />
                        {market.enderecos.length} {market.enderecos.length === 1 ? "loja" : "lojas"}
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <Package className="h-3.5 w-3.5" />
                        {total.toLocaleString("pt-BR")} produtos{buscaAplicada && " encontrados"}
                      </span>
                    </div>
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>

            {market.enderecos.length > 0 && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {market.enderecos.map((e) => (
                  <div key={e.id} className="rounded-lg border bg-muted/30 p-3 theme-transition">
                    <p className="font-medium flex items-center gap-1.5">
                      <Store className="h-4 w-4 text-primary" />
                      {e.descricao || "Loja"}
                    </p>
                    <p className="text-sm text-muted-foreground mt-1">{e.endereco}</p>
                  </div>
                ))}
              </div>
            )}

            <div className="space-y-3">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Buscar produto neste mercado..."
                  value={busca}
                  onChange={(e) => setBusca(e.target.value)}
                  className="pl-9"
                />
              </div>

              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Produto</TableHead>
                      <TableHead className="hidden sm:table-cell">Categoria</TableHead>
                      <TableHead className="text-right">Preço</TableHead>
                      <TableHead className="hidden md:table-cell text-right">Atualizado em</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {loading ? (
                      Array.from({ length: TAMANHO_PAGINA }).map((_, i) => (
                        <TableRow key={i}>
                          <TableCell colSpan={4}>
                            <div className="h-5 bg-muted animate-pulse rounded" />
                          </TableCell>
                        </TableRow>
                      ))
                    ) : produtos.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={4} className="h-24 text-center text-muted-foreground">
                          {erro
                            ? "Não foi possível carregar os produtos."
                            : buscaAplicada
                              ? `Nenhum produto encontrado para "${buscaAplicada}".`
                              : "Nenhum produto cadastrado neste mercado."}
                        </TableCell>
                      </TableRow>
                    ) : (
                      produtos.map((p) => (
                        <TableRow key={p.produtoId}>
                          <TableCell className="font-medium">{p.produto}</TableCell>
                          <TableCell className="hidden sm:table-cell">
                            {p.categoria && (
                              <Badge variant="secondary" className="text-xs theme-transition">
                                {p.categoria}
                              </Badge>
                            )}
                          </TableCell>
                          <TableCell className="text-right whitespace-nowrap">
                            R$ {p.preco.toFixed(2).replace(".", ",")}
                          </TableCell>
                          <TableCell className="hidden md:table-cell text-right text-muted-foreground">
                            {formatarData(p.ultimaAtualizacao)}
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>

              {totalPages > 1 && (
                <div className="flex flex-col items-center gap-1">
                  <ProductPagination page={page} totalPages={totalPages} onPageChange={setPage} />
                  <p className="text-xs text-muted-foreground">
                    Página {page} de {totalPages.toLocaleString("pt-BR")}
                  </p>
                </div>
              )}
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}
