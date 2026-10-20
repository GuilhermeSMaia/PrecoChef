"use client"

import { createContext, useContext, useState, useEffect, type ReactNode } from "react"
import { CheckCircle2 } from "lucide-react"
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
  const [carregado, setCarregado] = useState(false)
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
    setCarregado(true)
  }, [])

  // Salvar lista no localStorage quando mudar (só depois de carregar, senão sobrescreve a lista salva com [])
  useEffect(() => {
    if (!carregado) return
    localStorage.setItem("shoppingList", JSON.stringify(items))
  }, [items, carregado])

  const addItem = (produto: Produto, preco: Preco, mercado: Mercado) => {
    const existente = items.find((item) => item.produto.id === produto.id && item.mercado.id === mercado.id)

    // aviso pequeno de sucesso no canto inferior direito
    // o título do toast só aceita texto, por isso o ícone vai na descrição
    toast({
      description: (
        <span className="flex items-start gap-2">
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-[hsl(var(--best-price))]" />
          <span>
            <span className="block font-semibold">
              {existente ? `Quantidade: ${existente.quantidade + 1}` : "Adicionado à lista"}
            </span>
            {produto.produtoname} • {mercado.nome}
          </span>
        </span>
      ),
      duration: 2500,
      className: "p-3 pr-8 border-l-4 border-l-[hsl(var(--best-price))]",
    })

    // o updater precisa ser puro (o StrictMode o executa duas vezes): nada de toast nem de mutar o item aqui
    setItems((currentItems) => {
      const jaExiste = currentItems.some((item) => item.produto.id === produto.id && item.mercado.id === mercado.id)
      if (jaExiste) {
        return currentItems.map((item) =>
          item.produto.id === produto.id && item.mercado.id === mercado.id
            ? { ...item, quantidade: item.quantidade + 1 }
            : item,
        )
      }
      return [...currentItems, { produto, preco, mercado, quantidade: 1 }]
    })
  }

  const removeItem = (produtoId: string, mercadoId: string) => {
    const itemToRemove = items.find((item) => item.produto.id === produtoId && item.mercado.id === mercadoId)
    if (itemToRemove) {
      toast({
        title: "Produto removido",
        description: `${itemToRemove.produto.produtoname} (${itemToRemove.mercado.nome}) foi removido da sua lista`,
      })
    }
    setItems((currentItems) =>
      currentItems.filter((item) => !(item.produto.id === produtoId && item.mercado.id === mercadoId)),
    )
  }

  // Atualizar a função updateQuantity
  const updateQuantity = (produtoId: string, mercadoId: string, quantity: number) => {
    if (quantity <= 0) {
      removeItem(produtoId, mercadoId)
      return
    }

    setItems((currentItems) =>
      currentItems.map((item) =>
        item.produto.id === produtoId && item.mercado.id === mercadoId ? { ...item, quantidade: quantity } : item,
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

  const getTotalByMarket = () => {
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

  // economia = (preço mais caro - preço escolhido) para produtos que aparecem em mais de um mercado
  const getTotalSavings = () => {
    let savings = 0
    const precosPorProduto: { [key: string]: number[] } = {}

    items.forEach((item) => {
      ;(precosPorProduto[item.produto.id] ??= []).push(item.preco.preco)
    })

    Object.values(precosPorProduto).forEach((precos) => {
      if (precos.length > 1) {
        savings += Math.max(...precos) - Math.min(...precos)
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
