//listagem de mercados cadastrados

//endpoint get que traz os mercados cadastrados:{NEXT_API_URL}/mercados/getAll 
//NEXT_API_URL=http://0.0.0.0:8080
"use client"

import { useState, useEffect } from "react"
import type { ColumnDef } from "@tanstack/react-table"
import { MoreHorizontal, MapPin, Phone, Mail, Calendar, Edit, Trash2, Eye, ExternalLink, Store } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"
import { DataTable, createSortableHeader } from "@/components/ui/data-table"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"
import { useToast } from "@/components/ui/use-toast"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import Link from "next/link"
import { MarketDetailsDialog } from "@/components/market-details-dialog"
import { useAuth } from "@/context/auth-context"
import type { MercadoComEnderecos } from "@/lib/types"

type Market = MercadoComEnderecos

// "https://www.atacadao.com.br/" -> "atacadao.com.br"
const dominio = (url: string) => {
  try {
    return new URL(url).hostname.replace(/^www\./, "")
  } catch {
    return url
  }
}


export function MarketsDataTable() {
  const [markets, setMarkets] = useState<Market[]>([])
  const [loading, setLoading] = useState(true)
  const [mercadoDetalhes, setMercadoDetalhes] = useState<Market | null>(null)
  const { isAdmin } = useAuth()
  const { toast } = useToast()

  useEffect(() => {
  const fetchMarkets = async () => {
    try {
      const response = await fetch("http://localhost:8080/mercados/getAll")
      if (!response.ok) {
        throw new Error("Erro ao buscar mercados")
      }

      const data: Market[] = await response.json()
      setMarkets(data)
    } catch (error) {
      console.error("Erro ao carregar mercados:", error)
      toast({
        title: "Erro ao carregar mercados",
        description: "Não foi possível carregar os mercados da API.",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }
  fetchMarkets()
}, [])

  const handleEdit = (market: Market) => {
    toast({
      title: "Editar mercado",
      description: `Funcionalidade de edição para ${market.name} será implementada.`,
    })
  }


  const columns: ColumnDef<Market>[] = [
    {
      accessorKey: "name",
      header: createSortableHeader("Nome"),
      cell: ({ row }) => {
        const market = row.original
        return (
          <div className="flex items-center gap-3">
            <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
              <MapPin className="h-4 w-4 text-primary" />
            </div>
            <div className="min-w-0">
              <p className="font-medium truncate">{market.name}</p>
              <p className="text-sm text-muted-foreground truncate">
                {market.enderecos.length} {market.enderecos.length === 1 ? "loja" : "lojas"}
              </p>
            </div>
          </div>
        )
      },
    },
    {
      accessorKey: "url",
      header: "Site",
      cell: ({ row }) => {
        const url = row.original.url
        if (!url) return <span className="text-sm text-muted-foreground">—</span>
        return (
          <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 text-sm text-primary hover:underline"
          >
            {dominio(url)}
            <ExternalLink className="h-3 w-3" />
          </a>
        )
      },
    },
    {
      id: "enderecos",
      header: "Endereços",
      cell: ({ row }) => {
        const enderecos = row.original.enderecos
        if (enderecos.length === 0) return <span className="text-sm text-muted-foreground">—</span>
        return (
          <ul className="space-y-0.5 text-sm">
            {enderecos.map((e) => (
              <li key={e.id} className="flex items-start gap-1">
                <Store className="h-3 w-3 mt-1 flex-shrink-0 text-muted-foreground" />
                <span>
                  {e.descricao && <span className="font-medium">{e.descricao}: </span>}
                  <span className="text-muted-foreground">{e.endereco}</span>
                </span>
              </li>
            ))}
          </ul>
        )
      },
    },
    {
      id: "actions",
      enableHiding: false,
      cell: ({ row }) => {
        const market = row.original

        return (
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="icon"
              className="h-8 w-8 hover-lift theme-transition btn-orange-outline"
              title="Ver detalhes"
              onClick={() => setMercadoDetalhes(market)}
            >
              <span className="sr-only">Ver detalhes de {market.name}</span>
              <Eye className="h-4 w-4" />
            </Button>
            {/* editar e definir valores: só admin */}
            {isAdmin && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="h-8 w-8 p-0">
                  <span className="sr-only">Abrir menu</span>
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuSeparator />
                <Link href={`/mercados/${market.id}/editar`}>
                <DropdownMenuItem>
                  <Edit className="mr-2 h-4 w-4" />
                  Editar
                </DropdownMenuItem>
                </Link>
                <DropdownMenuItem>
                  <Link href={`/mercados/${market.id}/definir-valores`}>
                 
                    definir valores
                
                  </Link>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
            )}
          </div>
        )
      },
    },
  ]

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="h-8 bg-muted animate-pulse rounded" />
        <div className="h-64 bg-muted animate-pulse rounded" />
      </div>
    )
  }

  return (
    <>
    <DataTable
      columns={columns}
      data={markets}
      searchKey="name"
      searchPlaceholder="Buscar mercados..."
      title="Mercados Cadastrados"
      description="Gerencie todos os mercados registrados na plataforma"
    />
    <MarketDetailsDialog market={mercadoDetalhes} onClose={() => setMercadoDetalhes(null)} />
    </>
  )
}
