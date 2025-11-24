"use client"

import { useState } from "react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { ProductRegistrationForm } from "@/components/product-registration-form"
import { MarketRegistrationForm } from "@/components/market-registration-form"
import { Package, Store, Shield } from "lucide-react"

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState("products")

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
              <p className="text-xl text-muted-foreground">Gerencie produtos e mercados da plataforma</p>
            </div>
          </div>
        </div>

        <Card className="border-0 shadow-lg theme-transition">
          <CardHeader>
            <CardTitle>Cadastro de Dados</CardTitle>
            <CardDescription>
              Adicione novos produtos e mercados à plataforma para expandir as opções de comparação de preços.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
              <TabsList className="grid w-full grid-cols-2 mb-8">
                <TabsTrigger value="products" className="flex items-center gap-2">
                  <Package className="h-4 w-4" />
                  Produtos
                </TabsTrigger>
                <TabsTrigger value="markets" className="flex items-center gap-2">
                  <Store className="h-4 w-4" />
                  Mercados
                </TabsTrigger>
              </TabsList>

              <TabsContent value="products" className="animate-fade-in">
                <ProductRegistrationForm />
              </TabsContent>

              <TabsContent value="markets" className="animate-fade-in">
                <MarketRegistrationForm />
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
