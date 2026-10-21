"use client"

import type { ReactNode } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { Loader2, ShieldAlert } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { useAuth } from "@/context/auth-context"

// mostra o conteúdo só para admin. É só a camada visual: quem garante a regra é o backend (@RolesAllowed)
export function AdminOnly({ children }: { children: ReactNode }) {
  const { usuario, isAdmin, carregando } = useAuth()
  const pathname = usePathname()

  if (carregando) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (isAdmin) return <>{children}</>

  return (
    <div className="container mx-auto flex min-h-[60vh] items-center justify-center px-4 py-10">
      <Card className="w-full max-w-md text-center theme-transition">
        <CardHeader>
          <div className="mx-auto mb-2 h-12 w-12 rounded-full bg-destructive/10 flex items-center justify-center">
            <ShieldAlert className="h-6 w-6 text-destructive" />
          </div>
          <CardTitle>Acesso restrito</CardTitle>
          <CardDescription>
            {usuario
              ? "Sua conta não tem permissão de administrador."
              : "Entre com uma conta de administrador para acessar esta área."}
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 sm:flex-row sm:justify-center">
          {!usuario && (
            <Button asChild>
              <Link href={`/login?redirect=${encodeURIComponent(pathname)}&motivo=admin`}>Entrar</Link>
            </Button>
          )}
          <Button asChild variant="outline">
            <Link href="/">Voltar ao início</Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
