"use client"

import { createContext, useContext, useState, useEffect, type ReactNode } from "react"
import { useToast } from "@/components/ui/use-toast"

// Atualizar as importações e tipos
import type { ShoppingItem, Produto, Preco, Mercado } from "@/lib/types"

// Remover a definição antiga de Product e ShoppingItem

// Atualizar o tipo ShoppingListContextType
type ShoppingListContextType = {
  items: ShoppingItem[]
  addItem: (produto: Produto, preco: Preco, mercado: Mercado) => void
  removeItem: (productId: string, marketId: string) => void
  updateQuantity: (productId: string, marketId: string, quantity: number) => void
  clearList: () => void
  getTotalByMarket: () => { [key: string]: { mercado: Mercado; total: number } }
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
  const addItem = (produto: Produto, preco: Preco, mercado: Mercado) => {
    setItems((currentItems) => {
      // Verificar se o produto do mesmo mercado já está na lista
      const existingItemIndex = currentItems.findIndex(
        (item) => item.produto.id === produto.id && item.mercado.id === mercado.id,
      )

      if (existingItemIndex >= 0) {
        // Atualizar quantidade se já existir
        const updatedItems = [...currentItems]
        updatedItems[existingItemIndex].quantidade += 1

        toast({
          title: "Quantidade atualizada",
          description: `${produto.produtoname} (${mercado.nome}) agora tem ${updatedItems[existingItemIndex].quantidade} unidades`,
        })

        return updatedItems
      } else {
        // Adicionar novo item
        toast({
          title: "Produto adicionado",
          description: `${produto.produtoname} de ${mercado.nome} foi adicionado à sua lista`,
        })

        return [...currentItems, { produto, preco, mercado, quantidade: 1 }]
      }
    })
  }

  // Atualizar a função removeItem
  const removeItem = (produtoId: string, mercadoId: string) => {
    setItems((currentItems) => {
      const itemToRemove = currentItems.find((item) => item.produto.id === produtoId && item.mercado.id === mercadoId)
      if (itemToRemove) {
        toast({
          title: "Produto removido",
          description: `${itemToRemove.produto.produtoname} (${itemToRemove.mercado.nome}) foi removido da sua lista`,
        })
      }
      return currentItems.filter((item) => !(item.produto.id === produtoId && item.mercado.id === mercadoId))
    })
  }

  // Atualizar a função updateQuantity
  const updateQuantity = (produtoId: string, mercadoId: string, quantity: number) => {
    if (quantity <= 0) {
      removeItem(produtoId, mercadoId)
      return
    }

    setItems((currentItems) =>
      currentItems.map((item) =>
        item.produto.id === produtoId && item.mercado.id === mercadoId ? { ...item, quantity } : item,
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

  // Atualizar a função getTotalBymercado
  const getTotalByMercado = () => {
    const totals: { [key: string]: { mercado: Mercado; total: number } } = {}

    items.forEach((item) => {
      const marketId = item.mercado.id
      const itemTotal = item.preco.preco * item.quantidade

      if (totals[marketId]) {
        totals[marketId].total += itemTotal
      } else {
        totals[marketId] = {
          mercado: item.mercado,
          total: itemTotal,
        }
      }
    })

    return totals
  }

  const getTotalSavings = () => {
    let savings = 0
    const produtoGroups: { [key: string]: Produto[] } = {}

    items.forEach((item) => {
      const produtoNome = item.produto.produtoname
      if (!produtoGroups[produtoNome]) {
        produtoGroups[produtoNome] = []
      }
      produtoGroups[produtoNome].push(item.produto)
    })

    Object.values(produtoGroups).forEach((produto) => {
      if (produto.length > 1) {
        const prices = produto.map((p) => p.price)
        const maxPrice = Math.max(...prices)
        const minPrice = Math.min(...prices)
        savings += maxPrice - minPrice
      }
    })

    return savings
  }

  const getTotalItems = () => {
    return items.reduce((acc, item) => acc + item.quantidade, 0)
  }

  const getTotalValue = () => {
    return items.reduce((acc, item) => acc + item.preco.preco * item.quantidade, 0)
  }

  return (
    <ShoppingListContext.Provider
      value={{
        items,
        addItem,
        removeItem,
        updateQuantity,
        clearList,
        getTotalByMercado,
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
