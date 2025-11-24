//endpoint para encontrar o mercado pelo id: http://0.0.0.0:8080/mercados/getById/{id}
//Endpoint para encontrar o nome, medida e id dos produtos: http://0.0.0.0:8080/produtos/getAllNames
//endpoint para registrar o preço do produto no mercado: http://0.0.0.0:8080/preco/definir
//o endpoint precisa receber o {Long mercadoId, String nomeProduto, BigDecimal preco}

"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useToast } from "@/components/ui/use-toast";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";
import {
  DollarSign,
  Loader2,
  CheckCircle,
  Package,
  AlertCircle,
  ArrowLeft,
  Store,
  Home,
} from "lucide-react";
import { mockProducts } from "@/lib/mock-data";
import { mockMarkets } from "@/lib/mock-data";
import Link from "next/link";

const priceSchema = z.object({
  produtoId: z.string().min(1, "Selecione um produto"),
  preco: z
    .string()
    .min(1, "Digite o preço")
    .refine((val) => {
      const numVal = Number(val.replace(",", "."));
      return !isNaN(numVal) && numVal > 0 && numVal <= 999999;
    }, "Preço deve ser um número válido entre R$ 0,01 e R$ 999.999,00"),
  mercadoId: z.string().min(1, "Selecione um mercado"),
});

type PriceFormData = z.infer<typeof priceSchema>;

export default function SetProductPricePage() {
  const router = useRouter();
  const params = useParams();
  const id = params.mercadoId as string;

  const [products, setProducts] = useState<any>(null);
  const [market, setMarket] = useState<any>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  const form = useForm<PriceFormData>({
    resolver: zodResolver(priceSchema),
    defaultValues: {
      mercadoId: id,
      produtoId: "",
      preco: "",
    },
  });

  useEffect(() => {
    const fetchMarket = async () => {
      try {
        const res = await fetch(`http://localhost:8080/mercados/getDataById/${id}`);
        if (!res.ok) throw new Error("Erro ao buscar mercado");
        const data = await res.json();
        setMarket(data);
      } catch (err) {
        console.error(err);
        setError("Erro ao carregar mercado.");
      }
    };

    fetchMarket();
  }, [id]);

  useEffect(() => {
    // Buscar produtos
    const fetchProducts = async () => {
      try {
        const res = await fetch("http://localhost:8080/produtos/getAllNames");
        if (!res.ok) throw new Error("Erro ao buscar produtos");
        const data = await res.json();
        setProducts(data);
      } catch (err) {
        console.error(err);
        setError("Erro ao carregar produtos.");
      }
    };

    fetchProducts();
  }, []);

  const onSubmit = async (data: PriceFormData) => {
    setIsSubmitting(true);
    setError(null);

    try {
      const priceValue = Number(data.preco.replace(",", "."));

      const selectedProduct = products.find(
  (p: { id: number }) => p.id === Number(data.produtoId)
);
      if (!selectedProduct) {
        throw new Error("Produto não encontrado");
      }

      const response = await fetch("http://localhost:8080/preco/definir", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          mercadoId: id,
          produtoId: selectedProduct.id,
          preco: priceValue,
        }),
      });

      if (!response.ok) {
        throw new Error("Erro ao salvar o preço");
      }

      setIsSuccess(true);
      toast({
        title: "Preço definido com sucesso!",
        description: `${selectedProduct.name} agora custa R$ ${priceValue
          .toFixed(2)
          .replace(".", ",")} em ${market?.name}.`,
      });

      setTimeout(() => {
        router.push("/mercados");
      }, 2000);
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Erro desconhecido";
      setError(errorMessage);
      toast({
        title: "Erro ao definir preço",
        description: errorMessage,
        variant: "destructive",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatPrice = (value: string) => {
    // Remove non-numeric characters except decimal point and comma
    let numericValue = value.replace(/[^\d.,]/g, "");

    // Replace comma with dot for decimal
    numericValue = numericValue.replace(",", ".");

    // Ensure only one decimal point
    const parts = numericValue.split(".");
    if (parts.length > 2) {
      numericValue = parts[0] + "." + parts.slice(1).join("");
    }

    // Limit to 2 decimal places
    if (parts[1] && parts[1].length > 2) {
      numericValue = parts[0] + "." + parts[1].substring(0, 2);
    }

    return numericValue;
  };

  if (error && !market) {
    return (
      <div className="container mx-auto px-4 py-8">
        <Breadcrumb className="mb-6">
          <BreadcrumbList>
            <BreadcrumbItem>
              <BreadcrumbLink href="/">
                <Home className="h-4 w-4 mr-1" />
                Home
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbLink href="/mercados">Mercados</BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbLink>Definir Preço</BreadcrumbLink>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>

        <Alert variant="destructive" className="mb-4">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>

        <Button asChild variant="outline">
          <Link href="/mercados">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Voltar para Mercados
          </Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <Breadcrumb className="mb-6">
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink href="/">
              <Home className="h-4 w-4 mr-1" />
              Home
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink href="/mercados">Mercados</BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink>Definir Preço</BreadcrumbLink>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <div className="max-w-2xl mx-auto">
        <Card className="border-0 shadow-lg theme-transition">
          <CardHeader>
            <div className="flex items-center gap-3 mb-2">
              <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <DollarSign className="h-5 w-5 text-primary" />
              </div>
              <div>
                <CardTitle className="text-2xl">
                  Definir Preço do Produto
                </CardTitle>
                {market && (
                  <CardDescription className="flex items-center gap-1 mt-1">
                    <Store className="h-4 w-4" />
                    {market.name}
                  </CardDescription>
                )}
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {error && (
              <Alert variant="destructive" className="mb-6">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

           {isSuccess ? (
  <div className="py-8 text-center animate-scale-in">
    <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
    <h3 className="text-xl font-semibold mb-2">Preço Definido!</h3>
    <p className="text-muted-foreground mb-6">
      O preço foi salvo com sucesso.
    </p>

    {(() => {
      
      const selectedProduct = products.find(
  (p: { id: number }) => p.id === Number(form.getValues("produtoId"))
    );

      return (
        <div className="mt-4 p-4 bg-muted/30 rounded-lg mb-6">
          <p className="text-lg">
            <span className="font-medium">{selectedProduct?.name}</span>{" "}
            <span className="text-sm text-muted-foreground">
              ({selectedProduct?.medida})
            </span>{" "}
            - R${" "}
            {Number(form.getValues("preco").replace(",", "."))
              .toFixed(2)
              .replace(".", ",")}
          </p>
          <p className="text-sm text-muted-foreground">{market?.name}</p>
        </div>
      );
    })()}

    <p className="text-sm text-muted-foreground">
      Redirecionando para a lista de mercados...
    </p>
  </div>
) : (
  <Form {...form}>
    <form
      onSubmit={form.handleSubmit(onSubmit)}
      className="space-y-6"
    >
      <FormField
  control={form.control}
  name="produtoId"
  render={({ field }) => (
    <FormItem>
      <FormLabel className="flex items-center gap-2">
        <Package className="h-4 w-4" />
        Produto *
      </FormLabel>
      <Select
        onValueChange={(value) => field.onChange(value)}
        value={String(field.value)}
      >
        <FormControl>
          <SelectTrigger className="theme-transition">
            <SelectValue placeholder="Selecione um produto" />
          </SelectTrigger>
        </FormControl>
        <SelectContent className="max-h-[300px]">
          {products?.map((product: any) => (
            <SelectItem key={product.id} value={String(product.id)}>
              <div className="flex">
                <span className="font-medium">{product.name} </span>
                <span className="text-xs text-muted-foreground">
                  {product.medida}
                </span>
              </div>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <FormDescription>
        Escolha o produto para definir o preço
      </FormDescription>
      <FormMessage />
    </FormItem>
  )}
/>



                  <FormField
                    control={form.control}
                    name="preco"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel className="flex items-center gap-2">
                          <DollarSign className="h-4 w-4" />
                          Preço (R$) *
                        </FormLabel>
                        <FormControl>
                          <div className="relative">
                            <span className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground">
                              R$
                            </span>
                            <Input
                              placeholder="0,00"
                              {...field}
                              onChange={(e) => {
                                const formatted = formatPrice(e.target.value);
                                field.onChange(formatted);
                                console.log("Price input:", formatted);
                              }}
                              className="pl-10 theme-transition"
                              disabled={isSubmitting}
                            />
                          </div>
                        </FormControl>
                        <FormDescription>
                          Digite o preço atual do produto neste mercado
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </form>
              </Form>
            )}
          </CardContent>
          <CardFooter className="flex flex-col sm:flex-row gap-3">
            {!isSuccess && (
              <>
                <Button
                  variant="outline"
                  onClick={() => router.push("/mercados")}
                  className="flex-1 theme-transition"
                  disabled={isSubmitting}
                >
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  Cancelar
                </Button>
                <Button
                  onClick={form.handleSubmit(onSubmit)}
                  className="flex-1 theme-transition"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Salvando...
                    </>
                  ) : (
                    <>
                      <DollarSign className="mr-2 h-4 w-4" />
                      Definir Preço
                    </>
                  )}
                </Button>
              </>
            )}
            {isSuccess && (
              <Button asChild className="w-full">
                <Link href="/mercados">
                  <ArrowLeft className="mr-2 h-4 w-4" />
                  Voltar para Mercados
                </Link>
              </Button>
            )}
          </CardFooter>
        </Card>
      </div>
    </div>
  );
}
