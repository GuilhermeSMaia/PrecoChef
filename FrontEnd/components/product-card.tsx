"use client"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { useShoppingList } from "@/context/shopping-list-context"
import { PlusCircle, MapPin, Clock, Eye, Flame } from "lucide-react"
import Link from "next/link"
import { ProdutoWithPrices } from "@/lib/types"

interface ProdutoCardProps {
  produto: ProdutoWithPrices
}

export function ProductCard({ produto }: ProdutoCardProps) {
  const { addItem } = useShoppingList()

  const handleAddBestPrice = () => {
    if (produto.menorPreco) {
      addItem(produto, produto.menorPreco, produto.menorPreco.mercado)
    }
  }

  const formatLastUpdated = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString("pt-BR")
  }

  const savings =
    produto.precos.length > 1 ? Math.max(...produto.precos.map((p) => p.preco)) - (produto.menorPreco?.preco || 0) : 0

  return (
    <Card
      className={`group overflow-hidden hover-lift transition-all duration-300 border theme-transition ${
        produto.menorPreco ? "best-price-glow" : ""
      }`}
    >
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <CardTitle className="text-lg group-hover:text-primary transition-colors">{produto.produtoname}</CardTitle>
            <div className="flex items-center gap-2 text-sm text-muted-foreground mt-1">
              <Badge variant="secondary" className="text-xs theme-transition">
                {produto.categoria}
              </Badge>
              <span>•</span>
              <span>{produto.medida}</span>
            </div>
          </div>
          {savings > 0 && (
            <div className="flex items-center gap-1 text-xs savings-card px-2 py-1 rounded-full">
              <Flame className="h-3 w-3" />
              <span>-R$ {savings.toFixed(2)}</span>
            </div>
          )}
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {produto.menorPreco && (
          <div className="best-price-card p-4 rounded-xl">
            <div className="flex items-center justify-between mb-2">
              <Badge className="bg-best-price text-best-price-foreground hover:bg-best-price/90 animate-pulse-subtle theme-transition">
                Melhor preço
              </Badge>
              <span className="text-2xl font-bold text-best-price theme-transition">
                R$ {produto.menorPreco.preco.toFixed(2).replace(".", ",")}
              </span>
            </div>
            <div className="flex items-center gap-1 text-sm">
              <MapPin className="h-3 w-3" />
              <span className="font-medium">{produto.menorPreco.mercado}</span>
            </div>
            <div className="flex items-center gap-1 text-xs text-muted-foreground mt-1">
              <Clock className="h-3 w-3" />
              <span>Atualizado em {formatLastUpdated(produto.menorPreco.ultimaAtualizacao)}</span>
            </div>
          </div>
        )}

        {produto.precos.length > 1 && (
          <div className="space-y-2">
            <p className="text-sm font-medium text-muted-foreground">Outros preços:</p>
            <div className="space-y-1">
              {produto.precos.slice(1, 3).map((precoInfo) => (
                <div
                  key={precoInfo.id}
                  className="flex justify-between items-center text-sm p-2 rounded-lg bg-muted/30 theme-transition"
                >
                  <span className="text-muted-foreground">{precoInfo.mercado}</span>
                  <span className="font-medium">R$ {precoInfo.preco.toFixed(2).replace(".", ",")}</span>
                </div>
              ))}
              {produto.precos.length > 3 && (
                <p className="text-xs text-muted-foreground text-center py-1">
                  +{produto.precos.length - 3} mercado(s)
                </p>
              )}
            </div>
          </div>
        )}
      </CardContent>

      <CardFooter className="pt-0 flex gap-2">
        <Button
          className="flex-1 gap-2 hover-lift theme-transition"
          onClick={handleAddBestPrice}
          disabled={!produto.menorPreco}
        >
          <PlusCircle className="h-4 w-4" />
          Adicionar melhor
        </Button>
        <Link href={`/comparador?produto=${encodeURIComponent(produto.produtoname)}`}>
          <Button variant="outline" size="icon" className="hover-lift theme-transition btn-orange-outline">
            <Eye className="h-4 w-4" />
          </Button>
        </Link>
      </CardFooter>
    </Card>
  )
}
