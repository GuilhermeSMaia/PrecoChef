"use client"

import { createContext, useContext, useState, useEffect, type ReactNode } from "react"
import { useToast } from "@/components/ui/use-toast"

// Atualizar as importações e tipos
import type { Market, Product, Price, ShoppingItem } from "@/lib/types"

// Remover a definição antiga de Product e ShoppingItem

// Atualizar o tipo ShoppingListContextType
type ShoppingListContextType = {
  items: ShoppingItem[]
  addItem: (product: Product, price: Price, market: Market) => void
  removeItem: (productId: string, marketId: string) => void
  updateQuantity: (productId: string, marketId: string, quantity: number) => void
  clearList: () => void
  getTotalByMarket: () => { [key: string]: { market: Market; total: number } }
  getTotalSavings: () => number
  getTotalItems: () => number
  getTotalValue: () => number
}

const ShoppingListContext = createContext<ShoppingListContextType | undefined>(undefined)

export function ShoppingListProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ShoppingItem[]>([])
  const { toast } = useToast()

  // Carregar lista do localStorage quando o componente montar
  useEffect(() => {
    const savedItems = localStorage.getItem("shoppingList")
    if (savedItems) {
      try {
        setItems(JSON.parse(savedItems))
      } catch (e) {
        console.error("Erro ao carregar lista de compras:", e)
      }
    }
  }, [])

  // Salvar lista no localStorage quando mudar
  useEffect(() => {
    localStorage.setItem("shoppingList", JSON.stringify(items))
  }, [items])

  // Atualizar a função addItem
  const addItem = (product: Product, price: Price, market: Market) => {
    setItems((currentItems) => {
      // Verificar se o produto do mesmo mercado já está na lista
      const existingItemIndex = currentItems.findIndex(
        (item) => item.product.id === product.id && item.market.id === market.id,
      )

      if (existingItemIndex >= 0) {
        // Atualizar quantidade se já existir
        const updatedItems = [...currentItems]
        updatedItems[existingItemIndex].quantity += 1

        toast({
          title: "Quantidade atualizada",
          description: `${product.name} (${market.name}) agora tem ${updatedItems[existingItemIndex].quantity} unidades`,
        })

        return updatedItems
      } else {
        // Adicionar novo item
        toast({
          title: "Produto adicionado",
          description: `${product.name} de ${market.name} foi adicionado à sua lista`,
        })

        return [...currentItems, { product, price, market, quantity: 1 }]
      }
    })
  }

  // Atualizar a função removeItem
  const removeItem = (productId: string, marketId: string) => {
    setItems((currentItems) => {
      const itemToRemove = currentItems.find((item) => item.product.id === productId && item.market.id === marketId)
      if (itemToRemove) {
        toast({
          title: "Produto removido",
          description: `${itemToRemove.product.name} (${itemToRemove.market.name}) foi removido da sua lista`,
        })
      }
      return currentItems.filter((item) => !(item.product.id === productId && item.market.id === marketId))
    })
  }

  // Atualizar a função updateQuantity
  const updateQuantity = (productId: string, marketId: string, quantity: number) => {
    if (quantity <= 0) {
      removeItem(productId, marketId)
      return
    }

    setItems((currentItems) =>
      currentItems.map((item) =>
        item.product.id === productId && item.market.id === marketId ? { ...item, quantity } : item,
      ),
    )
  }

  const clearList = () => {
    setItems([])
    toast({
      title: "Lista limpa",
      description: "Todos os itens foram removidos da sua lista",
    })
  }

  // Atualizar a função getTotalByMarket
  const getTotalByMarket = () => {
    const totals: { [key: string]: { market: Market; total: number } } = {}

    items.forEach((item) => {
      const marketId = item.market.id
      const itemTotal = item.price.price * item.quantity

      if (totals[marketId]) {
        totals[marketId].total += itemTotal
      } else {
        totals[marketId] = {
          market: item.market,
          total: itemTotal,
        }
      }
    })

    return totals
  }

  const getTotalSavings = () => {
    // Implementação simplificada - em um cenário real, precisaríamos comparar com preços médios
    // ou com o preço mais alto de cada produto
    let savings = 0
    const productGroups: { [key: string]: Product[] } = {}

    // Agrupar produtos pelo nome para comparação
    items.forEach((item) => {
      const productName = item.product.name
      if (!productGroups[productName]) {
        productGroups[productName] = []
      }
      productGroups[productName].push(item.product)
    })

    // Calcular economia para cada grupo de produtos
    Object.values(productGroups).forEach((products) => {
      if (products.length > 1) {
        const prices = products.map((p) => p.price)
        const maxPrice = Math.max(...prices)
        const minPrice = Math.min(...prices)
        savings += maxPrice - minPrice
      }
    })

    return savings
  }

  // Adicionar novas funções utilitárias
  const getTotalItems = () => {
    return items.reduce((acc, item) => acc + item.quantity, 0)
  }

  const getTotalValue = () => {
    return items.reduce((acc, item) => acc + item.price.price * item.quantity, 0)
  }

  // Atualizar o provider value
  return (
    <ShoppingListContext.Provider
      value={{
        items,
        addItem,
        removeItem,
        updateQuantity,
        clearList,
        getTotalByMarket,
        getTotalSavings,
        getTotalItems,
        getTotalValue,
      }}
    >
      {children}
    </ShoppingListContext.Provider>
  )
}

export function useShoppingList() {
  const context = useContext(ShoppingListContext)
  if (context === undefined) {
    throw new Error("useShoppingList deve ser usado dentro de um ShoppingListProvider")
  }
  return context
}
