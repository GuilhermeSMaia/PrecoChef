"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { useShoppingList } from "@/context/shopping-list-context"
import { DataService } from "@/lib/data-service"
import type { Product, ProductWithPrices } from "@/lib/types"
import { PlusCircle, ShoppingBag, MapPin, Clock, TrendingDown } from "lucide-react"

interface ProductComparisonProps {
  selectedProduct: Product | null
}

export function ProductComparison({ selectedProduct }: ProductComparisonProps) {
  const [productData, setProductData] = useState<ProductWithPrices | null>(null)
  const [loading, setLoading] = useState(false)
  const { addItem } = useShoppingList()

  useEffect(() => {
    const loadProductComparison = async () => {
      if (!selectedProduct) {
        setProductData(null)
        return
      }

      setLoading(true)
      try {
        const data = await DataService.getProductComparison(selectedProduct.id)
        setProductData(data)
      } catch (error) {
        console.error("Erro ao carregar comparação:", error)
      } finally {
        setLoading(false)
      }
    }

    loadProductComparison()
  }, [selectedProduct])

  if (!selectedProduct) {
    return (
      <div className="text-center py-12 bg-muted/30 rounded-lg theme-transition">
        <ShoppingBag className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h3 className="text-xl font-medium mb-2">Nenhum produto selecionado</h3>
        <p className="text-muted-foreground">Digite o nome de um produto acima para comparar preços entre mercados.</p>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Card key={i} className="animate-pulse theme-transition">
            <CardHeader>
              <div className="h-4 bg-muted rounded w-3/4"></div>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="h-8 bg-muted rounded w-1/2"></div>
                <div className="h-4 bg-muted rounded w-full"></div>
                <div className="h-10 bg-muted rounded w-full"></div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  if (!productData || productData.prices.length === 0) {
    return (
      <div className="text-center py-12 bg-muted/30 rounded-lg theme-transition">
        <h3 className="text-xl font-medium mb-2">Produto não encontrado</h3>
        <p className="text-muted-foreground">
          Não encontramos "{selectedProduct.name}" em nenhum dos mercados cadastrados.
        </p>
      </div>
    )
  }

  const lowestPrice = productData.lowestPrice!
  const highestPrice = productData.prices[productData.prices.length - 1]
  const savings = highestPrice.price - lowestPrice.price

  const formatLastUpdated = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString("pt-BR")
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-semibold">{productData.name}</h2>
          <p className="text-muted-foreground">
            {productData.category} • {productData.unit}
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
        {productData.prices.map((priceInfo, index) => {
          const isLowestPrice = priceInfo.id === lowestPrice.id

          return (
            <Card
              key={priceInfo.id}
              className={`theme-transition hover-lift ${isLowestPrice ? "best-price-glow" : ""}`}
            >
              <CardHeader className="pb-3">
                <CardTitle className="flex justify-between items-start">
                  <div>
                    <span className="text-lg">{priceInfo.market.name}</span>
                    {isLowestPrice && (
                      <Badge className="ml-2 bg-best-price text-best-price-foreground hover:bg-best-price/90 theme-transition">
                        Melhor preço
                      </Badge>
                    )}
                  </div>
                </CardTitle>
                <div className="flex items-center gap-1 text-sm text-muted-foreground">
                  <MapPin className="h-3 w-3" />
                  <span>{priceInfo.market.address}</span>
                </div>
              </CardHeader>

              <CardContent className="space-y-4">
                <div className="text-center">
                  <p className={`text-3xl font-bold ${isLowestPrice ? "text-best-price" : ""} theme-transition`}>
                    R$ {priceInfo.price.toFixed(2).replace(".", ",")}
                  </p>
                  <div className="flex items-center justify-center gap-1 text-xs text-muted-foreground mt-1">
                    <Clock className="h-3 w-3" />
                    <span>Atualizado em {formatLastUpdated(priceInfo.last_updated)}</span>
                  </div>
                </div>

                {!isLowestPrice && savings > 0 && (
                  <div className="text-center text-sm text-muted-foreground">
                    R$ {(priceInfo.price - lowestPrice.price).toFixed(2).replace(".", ",")} mais caro
                  </div>
                )}

                <Button
                  className={`w-full gap-2 theme-transition ${isLowestPrice ? "" : "bg-muted/50 hover:bg-muted text-foreground"}`}
                  variant={isLowestPrice ? "default" : "outline"}
                  onClick={() => addItem(productData, priceInfo, priceInfo.market)}
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
