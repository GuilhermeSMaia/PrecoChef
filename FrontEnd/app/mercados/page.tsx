"use client"

import { MarketsDataTable } from "@/components/markets-data-table"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Store, Plus, BarChart3, Users } from "lucide-react"
import Link from "next/link"

export default function MercadosPage() {
  return (
    <div className="min-h-screen gradient-bg">
      <div className="container mx-auto px-4 py-8">
        <div className="mb-8 animate-fade-in">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
            <div>
              <h1 className="text-3xl md:text-4xl font-bold mb-2">Mercados Cadastrados</h1>
              <p className="text-xl text-muted-foreground">
                Visualize e gerencie todos os mercados registrados na plataforma
              </p>
            </div>
            <Link href="/admin">
              <Button className="gap-2 hover-lift">
                <Plus className="h-4 w-4" />
                Cadastrar Mercado
              </Button>
            </Link>
          </div>
        </div>

        <div className="animate-slide-up">
          <MarketsDataTable />
        </div>
      </div>
    </div>
  )
}
