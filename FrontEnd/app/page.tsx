import Link from "next/link"
import { ArrowRight, TrendingDown, ShoppingBag, BarChart3, Flame } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"

export default function Home() {
  return (
    <div className="min-h-screen gradient-bg">
      {/* Hero Section */}
      <div className="container mx-auto px-4 py-16 md:py-24">
        <div className="flex flex-col items-center text-center max-w-4xl mx-auto animate-fade-in">

          <h1 className="text-4xl md:text-6xl lg:text-7xl font-bold tracking-tight mb-6 bg-gradient-to-r from-foreground to-foreground/70 bg-clip-text text-transparent">
            Compare Preços e<span className="text-primary block">Economize Mais</span>
          </h1>

          <p className="text-xl md:text-2xl text-muted-foreground mb-8 max-w-3xl leading-relaxed">
            Encontre os melhores preços para seus produtos em diferentes mercados e crie sua lista de compras otimizada
            com economia inteligente.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 mb-16">
            <Link href="/produtos">
              <Button size="lg" className="gap-2 px-8 py-6 text-lg hover-lift orange-gradient-strong">
                Começar agora
                <ArrowRight className="h-5 w-5" />
              </Button>
            </Link>
          </div>

          
        </div>
      </div>

      {/* Features Section */}
      <div className="container mx-auto px-4 py-16">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">Como funciona</h2>
          <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
            Três passos simples para economizar nas suas compras
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-6xl mx-auto">
          <Card
            className="relative overflow-hidden hover-lift animate-slide-up border-0 shadow-lg orange-gradient"
            style={{ animationDelay: "0.1s" }}
          >
            <CardContent className="p-8 text-center">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary to-primary/50"></div>
              <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center mb-6 mx-auto">
                <TrendingDown className="h-8 w-8 text-primary" />
              </div>
              <h3 className="text-xl font-semibold mb-4">Compare Preços</h3>
              <p className="text-muted-foreground leading-relaxed">
                Veja o mesmo produto em diferentes mercados e descubra onde está mais barato com destaque visual para o
                melhor preço.
              </p>
            </CardContent>
          </Card>

          <Card
            className="relative overflow-hidden hover-lift animate-slide-up border-0 shadow-lg orange-gradient"
            style={{ animationDelay: "0.2s" }}
          >
            <CardContent className="p-8 text-center">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary to-primary/70"></div>
              <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center mb-6 mx-auto">
                <ShoppingBag className="h-8 w-8 text-primary" />
              </div>
              <h3 className="text-xl font-semibold mb-4">Monte sua Lista</h3>
              <p className="text-muted-foreground leading-relaxed">
                Adicione produtos à sua lista de compras automaticamente com os melhores preços e organize por mercado.
              </p>
            </CardContent>
          </Card>

          <Card
            className="relative overflow-hidden hover-lift animate-slide-up border-0 shadow-lg orange-gradient"
            style={{ animationDelay: "0.3s" }}
          >
            <CardContent className="p-8 text-center">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary to-primary/90"></div>
              <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center mb-6 mx-auto">
                <Flame className="h-8 w-8 text-primary" />
              </div>
              <h3 className="text-xl font-semibold mb-4">Economize</h3>
              <p className="text-muted-foreground leading-relaxed">
                Visualize quanto você economiza, exporte sua lista e leve ao mercado com total organização.
              </p>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* CTA Section */}
      <div className="container mx-auto px-4 py-16">
        <Card className="glass-effect border-0 shadow-2xl max-w-4xl mx-auto animate-fade-in orange-gradient">
          <CardContent className="p-12 text-center">
            <h2 className="text-3xl md:text-4xl font-bold mb-4">Pronto para economizar?</h2>
            <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
              Comece agora a comparar preços e criar suas listas de compras inteligentes.
            </p>
            <Link href="/produtos">
              <Button size="lg" className="gap-2 px-8 py-6 text-lg hover-lift orange-gradient-strong">
                Explorar produtos
                <ArrowRight className="h-5 w-5" />
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
