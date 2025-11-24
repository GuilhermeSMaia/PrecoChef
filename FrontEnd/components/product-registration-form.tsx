//Form para cadastro de produtos
//endpoint para cadastro: http://0.0.0.0:8080/produtos/create
//valores que precisa passar{ String nome, string categoria, string medida }
"use client"

import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useToast } from "@/components/ui/use-toast"
import { ImageUpload } from "@/components/image-upload"
import { Progress } from "@/components/ui/progress"
import { Loader2, Package, CheckCircle } from "lucide-react"

const productSchema = z.object({
  nome: z.string().min(2, "Nome deve ter pelo menos 2 caracteres").max(100, "Nome muito longo"),
  categoria: z.string().min(1, "Selecione uma categoria"),
  medida: z.string().min(1, "Selecione uma unidade"),
})

type ProductFormData = z.infer<typeof productSchema>

const categorias = [
  "Alimentos",
  "Laticínios",
  "Bebidas",
  "Limpeza",
  "Higiene",
  "Padaria",
  "Açougue",
  "Hortifruti",
  "Congelados",
  "Outros",
]

const medidas = ["kg", "g", "L", "ml", "unidade", "pacote", "caixa", "lata", "garrafa"]

export function ProductRegistrationForm() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitProgress, setSubmitProgress] = useState(0)
  const [isSuccess, setIsSuccess] = useState(false)
  const { toast } = useToast()

  const form = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      nome: "",
      categoria: "",
      medida: "",
    },
  })

 const onSubmit = async (data: ProductFormData) => {
  setIsSubmitting(true)
  setSubmitProgress(0)

  try {
    const progressInterval = setInterval(() => {
      setSubmitProgress((prev) => {
        if (prev >= 90) {
          clearInterval(progressInterval)
          return 90
        }
        return prev + 10
      })
    }, 200)

    const response = await fetch("http://localhost:8080/produtos/create", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    })

    clearInterval(progressInterval)

    if (!response.ok) {
      throw new Error("Erro ao cadastrar produto")
    }

    setSubmitProgress(100)

    setTimeout(() => {
      setIsSuccess(true)
      toast({
        title: "Produto cadastrado com sucesso!",
        description: `${data.nome} foi adicionado à plataforma.`,
      })

      setTimeout(() => {
        form.reset()
        setIsSuccess(false)
        setSubmitProgress(0)
      }, 2000)
    }, 500)
  } catch (error) {
    toast({
      title: "Erro ao cadastrar produto",
      description: "Tente novamente em alguns instantes.",
      variant: "destructive",
    })
  } finally {
    setIsSubmitting(false)
  }
}

  if (isSuccess) {
    return (
      <Card className="border-0 shadow-md theme-transition">
        <CardContent className="p-12 text-center">
          <div className="animate-scale-in">
            <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
            <h3 className="text-2xl font-semibold mb-2">Produto Cadastrado!</h3>
            <p className="text-muted-foreground">O produto foi adicionado com sucesso à plataforma.</p>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="border-0 shadow-md theme-transition">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Package className="h-5 w-5" />
          Cadastrar Produto
        </CardTitle>
        <CardDescription>Adicione um novo produto para comparação de preços entre mercados.</CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <FormField
                control={form.control}
                name="nome"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nome do Produto *</FormLabel>
                    <FormControl>
                      <Input placeholder="Ex: Arroz Integral" {...field} className="theme-transition" />
                    </FormControl>
                    <FormDescription>Nome comercial do produto</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="categoria"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Categoria *</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger className="theme-transition">
                          <SelectValue placeholder="Selecione uma categoria" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {categorias.map((categoria) => (
                          <SelectItem key={categoria} value={categoria}>
                            {categoria}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormDescription>Categoria do produto</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="medida"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Unidade de Medida *</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger className="theme-transition">
                        <SelectValue placeholder="Selecione uma unidade" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {medidas.map((medida) => (
                        <SelectItem key={medida} value={medida}>
                          {medida}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormDescription>Como o produto é vendido (kg, L, unidade, etc.)</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            {isSubmitting && (
              <div className="space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span>Cadastrando produto...</span>
                  <span>{submitProgress}%</span>
                </div>
                <Progress value={submitProgress} className="w-full" />
              </div>
            )}

            <Button type="submit" className="w-full theme-transition" disabled={isSubmitting}>
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Cadastrando...
                </>
              ) : (
                "Cadastrar Produto"
              )}
            </Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  )
}
