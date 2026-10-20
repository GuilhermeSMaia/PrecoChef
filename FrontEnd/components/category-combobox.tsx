"use client"

import { useState } from "react"
import { Check, ChevronsUpDown, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import type { CategoriaComContagem } from "@/hooks/use-categorias"
import { cn } from "@/lib/utils"

// "Açougue" -> "acougue": a busca não depende de acento nem de maiúsculas
const normalizar = (texto: string) =>
  texto
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()

interface CategoryComboboxProps {
  categorias: CategoriaComContagem[]
  value: string // "" = todas
  onChange: (categoria: string) => void
  carregando?: boolean
  className?: string
}

export function CategoryCombobox({ categorias, value, onChange, carregando, className }: CategoryComboboxProps) {
  const [aberto, setAberto] = useState(false)

  const escolher = (categoria: string) => {
    onChange(categoria)
    setAberto(false)
  }

  return (
    <Popover open={aberto} onOpenChange={setAberto}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          role="combobox"
          aria-expanded={aberto}
          disabled={carregando}
          className={cn(
            "justify-between h-12 px-3 font-normal bg-muted/50 hover:bg-muted transition-colors",
            !value && "text-muted-foreground",
            className,
          )}
        >
          <span className="truncate">{carregando ? "Carregando..." : value || "Todas as categorias"}</span>
          {value ? (
            // limpa só a categoria, sem abrir a lista
            <X
              className="ml-2 h-4 w-4 shrink-0 opacity-60 hover:opacity-100"
              aria-label="Limpar categoria"
              onPointerDown={(e) => {
                e.preventDefault()
                e.stopPropagation()
                onChange("")
              }}
            />
          ) : (
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[--radix-popover-trigger-width] min-w-[260px] p-0" align="start">
        <Command filter={(item, busca) => (normalizar(item).includes(normalizar(busca)) ? 1 : 0)}>
          {/* busca numa caixa própria, com respiro, e o destaque de foco na caixa em vez de colado no texto */}
          <CommandInput
            placeholder="Buscar categoria..."
            wrapperClassName="m-2 mb-1 rounded-md border border-input bg-muted/40 transition-colors focus-within:border-primary/60 focus-within:bg-background"
            className="h-9 py-2 focus-visible:outline-none"
          />
          <CommandList className="max-h-72 border-t">
            <CommandEmpty>Nenhuma categoria encontrada.</CommandEmpty>
            <CommandGroup>
              <CommandItem value="Todas as categorias" onSelect={() => escolher("")}>
                <Check className={cn("h-4 w-4", value ? "opacity-0" : "opacity-100")} />
                Todas as categorias
              </CommandItem>
              {categorias.map((c) => (
                <CommandItem key={c.nome} value={c.nome} onSelect={() => escolher(c.nome)}>
                  <Check className={cn("h-4 w-4", value === c.nome ? "opacity-100" : "opacity-0")} />
                  <span className="flex-1 truncate">{c.nome}</span>
                  <span className="text-xs text-muted-foreground tabular-nums">
                    {c.produtos.toLocaleString("pt-BR")}
                  </span>
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  )
}
