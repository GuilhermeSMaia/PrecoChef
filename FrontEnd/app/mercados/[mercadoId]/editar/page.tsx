"use client"

import { useState } from "react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { ProductRegistrationForm } from "@/components/product-registration-form"
import { MarketRegistrationForm } from "@/components/market-registration-form"
import { Package, Store, Shield } from "lucide-react"

import { useParams, useRouter } from "next/navigation"

export default function AdminPage() {
  const router = useRouter();
  const params = useParams();
  const id = params.mercadoId as string;
  return (
    <div className="min-h-screen gradient-bg">
      <div className="container mx-auto px-4 py-8">
        <div className="mb-8 animate-fade-in">
          <div className="flex items-center gap-3 mb-4">
            <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center">
              <Shield className="h-6 w-6 text-primary" />
            </div>
            <div>
              <h1 className="text-3xl md:text-4xl font-bold">Painel Administrativo</h1>
              <p className="text-xl text-muted-foreground">Gerencie mercados da plataforma</p>
            </div>
          </div>
        </div>

        <Card className="border-0 shadow-lg theme-transition">
          <CardHeader>
            <CardTitle>Cadastro de Dados</CardTitle>
            <CardDescription>
              Atualize as informações do mercado
            </CardDescription>
          </CardHeader>
          <CardContent>
                <MarketRegistrationForm id={id}/>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
