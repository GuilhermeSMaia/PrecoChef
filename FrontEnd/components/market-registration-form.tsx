"use client"

import { useEffect, useState } from "react"
import { useFieldArray, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useToast } from "@/components/ui/use-toast"
import { Progress } from "@/components/ui/progress"
import { Loader2, Store, CheckCircle, MapPin, Phone, Mail, Plus, Trash2 } from "lucide-react"

const EnderecoSchema = z.object({
  id: z.number().optional(), // id do endereço já salvo (edição)
  descricao: z.string().max(100, "Descrição muito longa").optional(),
  endereco: z.string().min(5, "Endereço deve ter pelo menos 5 caracteres").max(255, "Endereço muito longo"),
})

const MarketSchema = z.object({
  mercadoId: z.string().optional(),
  name: z.string().min(3, "Nome deve ter pelo menos 3 caracteres").max(100, "Nome muito longo"),
  enderecos: z.array(EnderecoSchema),
})

type MarketRegistrationFormProps = {
  id?: string
}
type MarketFormData = z.infer<typeof MarketSchema>

export function MarketRegistrationForm({ id }: MarketRegistrationFormProps) {
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitProgress, setSubmitProgress] = useState(0)
  const [isSuccess, setIsSuccess] = useState(false)
  const { toast } = useToast()
  
  const form = useForm<MarketFormData>({
    resolver: zodResolver(MarketSchema),
    defaultValues: {
      mercadoId: id ?? "",
      name: "",
      enderecos: [{ descricao: "", endereco: "" }],
    },
  })

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "enderecos",
  })

   useEffect(() => {
    const fetchMarket = async () => {
      try {
        const response = await fetch(`http://localhost:8080/mercados/getDataById/${id}`)
        if (!response.ok) {
          throw new Error("Erro ao buscar mercado")
        }

        const data = await response.json()
        form.reset({
          mercadoId: id,
          name: data.name || "",
          enderecos: (data.enderecos ?? []).map((e: { id: number; descricao?: string; endereco: string }) => ({
            id: e.id,
            descricao: e.descricao ?? "",
            endereco: e.endereco ?? "",
          })),
        })
      } catch (error) {
        toast({
          title: "Erro ao buscar mercado",
          description: "Não foi possível carregar os dados do mercado.",
          variant: "destructive",
        })
      } finally {
        setIsLoading(false)
      }
    }

    if (id) {
      fetchMarket()
    }
  }, [id, form, toast])


  const onSubmit = async (data: MarketFormData) => {
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

    const response = await fetch("http://localhost:8080/mercados/create", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        ...data,
        mercadoId: data.mercadoId ? Number(data.mercadoId) : null,
      }),
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
        description: `${data.name} foi adicionado à plataforma.`,
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
            <h3 className="text-2xl font-semibold mb-2">Mercado Cadastrado!</h3>
            <p className="text-muted-foreground">O mercado foi adicionado com sucesso à plataforma.</p>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="border-0 shadow-md theme-transition">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Store className="h-5 w-5" />
          Cadastrar Mercado
        </CardTitle>
        <CardDescription>Adicione um novo mercado para expandir as opções de comparação de preços.</CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nome do Mercado *</FormLabel>
                    <FormControl>
                      <Input placeholder="Ex: Supermercado Central" {...field} className="theme-transition" />
                    </FormControl>
                    <FormDescription>Nome comercial do estabelecimento</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="font-medium flex items-center gap-2">
                    <MapPin className="h-4 w-4" />
                    Endereços
                  </h4>
                  <p className="text-sm text-muted-foreground">Adicione um endereço para cada loja/unidade do mercado</p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => append({ descricao: "", endereco: "" })}
                >
                  <Plus className="mr-1 h-4 w-4" />
                  Adicionar endereço
                </Button>
              </div>

              {fields.map((item, index) => (
                <div key={item.id} className="grid grid-cols-1 md:grid-cols-[1fr_2fr_auto] gap-4 items-start">
                  <FormField
                    control={form.control}
                    name={`enderecos.${index}.descricao`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Unidade</FormLabel>
                        <FormControl>
                          <Input placeholder="Ex: Loja Centro" {...field} className="theme-transition" />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name={`enderecos.${index}.endereco`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Endereço *</FormLabel>
                        <FormControl>
                          <Input placeholder="Ex: Av. Jatuarana, 1000 - Porto Velho" {...field} className="theme-transition" />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="md:mt-8"
                    onClick={() => remove(index)}
                    aria-label="Remover endereço"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              ))}
            </div>

            {isSubmitting && (
              <div className="space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span>Cadastrando mercado...</span>
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
                "Cadastrar Mercado"
              )}
            </Button>
          </form>
        </Form>
      </CardContent>
    </Card>
  )
}
