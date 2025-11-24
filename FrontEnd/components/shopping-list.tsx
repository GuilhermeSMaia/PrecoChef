"use client"

import { useState } from "react"
import { useShoppingList } from "@/context/shopping-list-context"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import { Trash2, FileDown, Save, ShoppingCart, Plus, Minus } from "lucide-react"
import { useToast } from "@/components/ui/use-toast"

export function ShoppingList() {
  const {
    items,
    removeItem,
    updateQuantity,
    clearList,
    getTotalByMarket,
    getTotalSavings,
    getTotalItems,
    getTotalValue,
  } = useShoppingList()
  const { toast } = useToast()
  const [listName, setListName] = useState("Minha Lista de Compras")

  const handleExportPDF = () => {
    // Em um app real, isso geraria um PDF
    toast({
      title: "PDF gerado",
      description: "Sua lista de compras foi exportada como PDF.",
    })
  }

  const handleSaveList = () => {
    // Em um app real, isso salvaria a lista no banco de dados
    toast({
      title: "Lista salva",
      description: "Sua lista de compras foi salva com sucesso.",
    })
  }

  const totalByMarket = getTotalByMarket()
  const totalSavings = getTotalSavings()
  const totalItems = getTotalItems()
  const totalValue = getTotalValue()

  // Agrupar itens por mercado
  const itemsByMarket: Record<string, typeof items> = {}
  items.forEach((item) => {
    const marketId = item.market.id
    if (!itemsByMarket[marketId]) {
      itemsByMarket[marketId] = []
    }
    itemsByMarket[marketId].push(item)
  })

  if (items.length === 0) {
    return (
      <div className="text-center py-12 bg-muted/30 rounded-lg theme-transition">
        <ShoppingCart className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
        <h3 className="text-xl font-medium mb-2">Sua lista está vazia</h3>
        <p className="text-muted-foreground mb-6">Adicione produtos à sua lista para começar a economizar.</p>
        <Button asChild className="theme-transition">
          <a href="/produtos">Ver produtos</a>
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <Input
          value={listName}
          onChange={(e) => setListName(e.target.value)}
          className="text-lg font-medium max-w-md theme-transition"
        />

        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={handleSaveList} className="gap-1 theme-transition">
            <Save className="h-4 w-4" />
            Salvar
          </Button>
          <Button variant="outline" size="sm" onClick={handleExportPDF} className="gap-1 theme-transition">
            <FileDown className="h-4 w-4" />
            Exportar PDF
          </Button>
          <Button variant="outline" size="sm" onClick={clearList} className="gap-1 theme-transition">
            <Trash2 className="h-4 w-4" />
            Limpar
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          {Object.entries(itemsByMarket).map(([marketId, marketItems]) => {
            const marketData = totalByMarket[marketId]
            if (!marketData) return null

            return (
              <Card key={marketId} className="theme-transition hover-lift">
                <CardHeader className="pb-2">
                  <CardTitle className="flex justify-between items-center">
                    <div>
                      <span>{marketData.market.name}</span>
                      <p className="text-sm text-muted-foreground font-normal">{marketData.market.address}</p>
                    </div>
                    <span>R$ {marketData.total.toFixed(2).replace(".", ",")}</span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <ul className="space-y-2">
                    {marketItems.map((item) => (
                      <li
                        key={`${item.product.id}-${item.market.id}`}
                        className="flex items-center justify-between py-2 border-b last:border-0 theme-transition"
                      >
                        <div className="flex-1">
                          <p className="font-medium">{item.product.name}</p>
                          <p className="text-sm text-muted-foreground">
                            {item.product.unit} - R$ {item.price.price.toFixed(2).replace(".", ",")}
                          </p>
                        </div>

                        <div className="flex items-center gap-2">
                          <Button
                            variant="outline"
                            size="icon"
                            className="h-8 w-8 theme-transition"
                            onClick={() => updateQuantity(item.product.id, item.market.id, item.quantity - 1)}
                          >
                            <Minus className="h-3 w-3" />
                          </Button>

                          <span className="w-8 text-center">{item.quantity}</span>

                          <Button
                            variant="outline"
                            size="icon"
                            className="h-8 w-8 theme-transition"
                            onClick={() => updateQuantity(item.product.id, item.market.id, item.quantity + 1)}
                          >
                            <Plus className="h-3 w-3" />
                          </Button>

                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-destructive theme-transition"
                            onClick={() => removeItem(item.product.id, item.market.id)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </li>
                    ))}
                  </ul>
                </CardContent>
              </Card>
            )
          })}
        </div>

        <div>
          <Card className="theme-transition hover-lift">
            <CardHeader>
              <CardTitle>Resumo</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex justify-between">
                <span>Total de itens:</span>
                <span>{totalItems}</span>
              </div>

              <Separator className="theme-transition" />

              <div className="space-y-2">
                <p className="font-medium">Total por mercado:</p>
                {Object.entries(totalByMarket).map(([marketId, marketData]) => (
                  <div key={marketId} className="flex justify-between text-sm">
                    <span>{marketData.market.name}:</span>
                    <span>R$ {marketData.total.toFixed(2).replace(".", ",")}</span>
                  </div>
                ))}
              </div>

              <Separator className="theme-transition" />

              <div className="flex justify-between font-bold">
                <span>Valor total:</span>
                <span>R$ {totalValue.toFixed(2).replace(".", ",")}</span>
              </div>

              {totalSavings > 0 && (
                <div className="savings-card p-3 rounded-md">
                  <p className="font-medium">Economia estimada: R$ {totalSavings.toFixed(2).replace(".", ",")}</p>
                </div>
              )}
            </CardContent>
            <CardFooter>
              <Button className="w-full theme-transition">Finalizar compra</Button>
            </CardFooter>
          </Card>
        </div>
      </div>
    </div>
  )
}
