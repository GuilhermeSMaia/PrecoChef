"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { useShoppingList } from "@/context/shopping-list-context"
import type { ProdutoWithPrices } from "@/lib/types"
import { formatarData } from "@/lib/utils"
import { PlusCircle, ShoppingBag, Clock, TrendingDown } from "lucide-react"

interface ProductComparisonProps {
  // já vem do seletor com os preços ordenados do menor para o maior
  selectedProduct: ProdutoWithPrices | null
}

export function ProductComparison({ selectedProduct }: ProductComparisonProps) {
  const { addItem } = useShoppingList()

  if (!selectedProduct) {
    return (
      <div className="text-center py-12 bg-muted/30 rounded-lg theme-transition">
        <ShoppingBag className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h3 className="text-xl font-medium mb-2">Nenhum produto selecionado</h3>
        <p className="text-muted-foreground">Digite o nome de um produto acima para comparar preços entre mercados.</p>
      </div>
    )
  }

  const precos = selectedProduct.precos

  if (precos.length === 0) {
    return (
      <div className="text-center py-12 bg-muted/30 rounded-lg theme-transition">
        <h3 className="text-xl font-medium mb-2">Sem preços cadastrados</h3>
        <p className="text-muted-foreground">
          "{selectedProduct.produtoname}" ainda não tem preço em nenhum dos mercados cadastrados.
        </p>
      </div>
    )
  }

  const lowestPrice = precos[0]
  const highestPrice = precos[precos.length - 1]
  const savings = highestPrice.preco - lowestPrice.preco

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-semibold">{selectedProduct.produtoname}</h2>
          <p className="text-muted-foreground">
            {selectedProduct.categoria} • {selectedProduct.medida}
          </p>
        </div>

        {savings > 0 && (
          <div className="savings-card p-3 rounded-lg">
            <div className="flex items-center gap-2">
              <TrendingDown className="h-4 w-4" />
              <span className="font-medium">Economia de até R$ {savings.toFixed(2).replace(".", ",")}</span>
            </div>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {precos.map((priceInfo) => {
          const isLowestPrice = priceInfo.preco === lowestPrice.preco

          return (
            <Card
              key={priceInfo.mercado}
              className={`theme-transition hover-lift ${isLowestPrice ? "best-price-glow" : ""}`}
            >
              <CardHeader className="pb-3">
                <CardTitle className="flex justify-between items-start">
                  <div>
                    <span className="text-lg">{priceInfo.mercado}</span>
                    {isLowestPrice && (
                      <Badge className="ml-2 bg-best-price text-best-price-foreground hover:bg-best-price/90 theme-transition">
                        Melhor preço
                      </Badge>
                    )}
                  </div>
                </CardTitle>
              </CardHeader>

              <CardContent className="space-y-4">
                <div className="text-center">
                  <p className={`text-3xl font-bold ${isLowestPrice ? "text-best-price" : ""} theme-transition`}>
                    R$ {priceInfo.preco.toFixed(2).replace(".", ",")}
                  </p>
                  <div className="flex items-center justify-center gap-1 text-xs text-muted-foreground mt-1">
                    <Clock className="h-3 w-3" />
                    <span>Atualizado em {formatarData(priceInfo.ultimaAtualizacao)}</span>
                  </div>
                </div>

                {!isLowestPrice && (
                  <div className="text-center text-sm text-muted-foreground">
                    R$ {(priceInfo.preco - lowestPrice.preco).toFixed(2).replace(".", ",")} mais caro
                  </div>
                )}

                <Button
                  className={`w-full gap-2 theme-transition ${isLowestPrice ? "" : "bg-muted/50 hover:bg-muted text-foreground"}`}
                  variant={isLowestPrice ? "default" : "outline"}
                  // o backend manda só o nome do mercado; o contexto espera um Mercado { id, nome }
                  onClick={() => addItem(selectedProduct, priceInfo, { id: priceInfo.mercado, nome: priceInfo.mercado })}
                >
                  <PlusCircle className="h-4 w-4" />
                  Adicionar à lista
                </Button>
              </CardContent>
            </Card>
          )
        })}
      </div>
    </div>
  )
}
