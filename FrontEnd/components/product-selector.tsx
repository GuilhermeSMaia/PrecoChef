"use client"

import { useState, useEffect } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Search } from "lucide-react"
import { DataService } from "@/lib/data-service"
import type { Product } from "@/lib/types"

interface ProductSelectorProps {
  onProductSelect: (product: Product | null) => void
}

export function ProductSelector({ onProductSelect }: ProductSelectorProps) {
  const [searchTerm, setSearchTerm] = useState("")
  const [suggestions, setSuggestions] = useState<Product[]>([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)

  useEffect(() => {
    const searchProducts = async () => {
      if (searchTerm.length > 1) {
        const products = await DataService.searchProducts(searchTerm)
        setSuggestions(products)
      } else {
        setSuggestions([])
      }
    }

    const debounceTimer = setTimeout(searchProducts, 300)
    return () => clearTimeout(debounceTimer)
  }, [searchTerm])

  const handleSuggestionClick = (product: Product) => {
    setSearchTerm(product.name)
    setSelectedProduct(product)
    onProductSelect(product)
    setShowSuggestions(false)
  }

  const handleSearch = () => {
    const exactMatch = suggestions.find((product) => product.name.toLowerCase() === searchTerm.toLowerCase())

    if (exactMatch) {
      setSelectedProduct(exactMatch)
      onProductSelect(exactMatch)
    } else if (suggestions.length > 0) {
      const firstMatch = suggestions[0]
      setSelectedProduct(firstMatch)
      onProductSelect(firstMatch)
      setSearchTerm(firstMatch.name)
    }
    setShowSuggestions(false)
  }

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
                if (!e.target.value) {
                  setSelectedProduct(null)
                  onProductSelect(null)
                }
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
                onClick={() => handleSuggestionClick(product)}
              >
                <div className="font-medium">{product.name}</div>
                <div className="text-sm text-muted-foreground">
                  {product.category} • {product.unit}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {selectedProduct && (
        <div className="bg-muted/30 p-4 rounded-lg">
          <p className="text-sm text-muted-foreground">Comparando preços para:</p>
          <p className="font-medium text-lg">{selectedProduct.name}</p>
          <p className="text-sm text-muted-foreground">
            {selectedProduct.category} • {selectedProduct.unit}
          </p>
        </div>
      )}
    </div>
  )
}
