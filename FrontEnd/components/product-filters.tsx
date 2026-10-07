"use client"

import { useState, useEffect } from "react"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Card, CardContent } from "@/components/ui/card"
import { Search, X, Filter } from "lucide-react"

const categories = ["Alimentos", "Laticínios", "Bebidas", "Limpeza", "Higiene"]

interface ProductFiltersProps {
  onFiltersChange: (filters: { search: string; category: string; sortBy: string }) => void
}

export function ProductFilters({ onFiltersChange }: ProductFiltersProps) {
  const [search, setSearch] = useState("")
  const [category, setCategory] = useState("")
  const [sort, setSort] = useState("")
  const [isFiltersOpen, setIsFiltersOpen] = useState(false)

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      onFiltersChange({ search, category, sortBy: sort })
    }, 300)

    return () => clearTimeout(timeoutId)
  }, [search, category, sort])

  const clearFilters = () => {
    setSearch("")
    setCategory("")
    setSort("")
  }

  const hasActiveFilters = search || category || sort

  return (
    <Card className="border-0 shadow-md">
      <CardContent className="p-6">
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar produtos..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-10 h-12 border-0 bg-muted/50 focus:bg-background transition-colors"
              />
            </div>
            <Button
              variant="outline"
              onClick={() => setIsFiltersOpen(!isFiltersOpen)}
              className="sm:hidden gap-2 h-12 hover-lift"
            >
              <Filter className="h-4 w-4" />
              Filtros
              {hasActiveFilters && (
                <span className="bg-primary text-primary-foreground text-xs px-2 py-1 rounded-full">
                  {[search, category, sort].filter(Boolean).length}
                </span>
              )}
            </Button>
          </div>

          <div
            className={`space-y-4 sm:space-y-0 sm:flex sm:gap-4 sm:items-center ${
              isFiltersOpen ? "block animate-slide-up" : "hidden sm:flex"
            }`}
          >
            <div className="flex flex-col sm:flex-row gap-4 flex-1">
              <Select value={category} onValueChange={setCategory}>
                <SelectTrigger className="w-full sm:w-[200px] h-12 border-0 bg-muted/50 hover:bg-muted transition-colors">
                  <SelectValue placeholder="Todas as categorias" />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((cat) => (
                    <SelectItem key={cat} value={cat}>
                      {cat}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select value={sort} onValueChange={setSort}>
                <SelectTrigger className="w-full sm:w-[200px] h-12 border-0 bg-muted/50 hover:bg-muted transition-colors">
                  <SelectValue placeholder="Ordenar por" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="price-asc">Menor preço</SelectItem>
                  <SelectItem value="price-desc">Maior preço</SelectItem>
                  <SelectItem value="name-asc">Nome (A-Z)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {hasActiveFilters && (
              <Button
                variant="ghost"
                onClick={clearFilters}
                className="gap-2 text-muted-foreground hover:text-foreground hover-lift"
              >
                <X className="h-4 w-4" />
                Limpar filtros
              </Button>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
