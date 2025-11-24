//listagem de mercados cadastrados

//endpoint get que traz os mercados cadastrados:{NEXT_API_URL}/mercados/getAll 
//NEXT_API_URL=http://0.0.0.0:8080
"use client"

import { useState, useEffect } from "react"
import type { ColumnDef } from "@tanstack/react-table"
import { MoreHorizontal, MapPin, Phone, Mail, Calendar, Edit, Trash2, Eye } from "lucide-react"
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

interface Market {
  id: string
  name: string
  address: string
  phone: string
  email?: string
  description?: string
  logo?: string
  createdAt: string
  status: "active" | "inactive" | "pending"
}


export function MarketsDataTable() {
  const [markets, setMarkets] = useState<Market[]>([])
  const [loading, setLoading] = useState(true)
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
console.log(markets)

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
              <p className="text-sm text-muted-foreground truncate">{market.phone}</p>
            </div>
          </div>
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
    <DataTable
      columns={columns}
      data={markets}
      searchKey="name"
      searchPlaceholder="Buscar mercados..."
      title="Mercados Cadastrados"
      description="Gerencie todos os mercados registrados na plataforma"
    />
  )
}
