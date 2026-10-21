"use client"

import { useState, type FormEvent } from "react"
import Link from "next/link"
import { useRouter, useSearchParams } from "next/navigation"
import { Check, Eye, EyeOff, Flame, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { PasswordStrength, senhaAtendeObrigatorias } from "@/components/password-strength"
import { useAuth } from "@/context/auth-context"
import { cn } from "@/lib/utils"

// só aceita caminhos internos, para o ?redirect= não levar a outro site
function destinoSeguro(redirect: string | null) {
  return redirect && redirect.startsWith("/") && !redirect.startsWith("//") ? redirect : "/"
}

const MOTIVOS: Record<string, string> = {
  salvar: "Entre na sua conta para salvar sua lista de compras.",
  admin: "Esta área é só para administradores.",
}

export function AuthForm({ modo }: { modo: "login" | "cadastro" }) {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { login, registrar } = useAuth()

  const [nome, setNome] = useState("")
  const [email, setEmail] = useState("")
  const [senha, setSenha] = useState("")
  const [confirmacao, setConfirmacao] = useState("")
  const [mostrarSenha, setMostrarSenha] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  const cadastro = modo === "cadastro"
  const destino = destinoSeguro(searchParams.get("redirect"))
  const motivo = MOTIVOS[searchParams.get("motivo") ?? ""]
  // mantém o redirect/motivo ao alternar entre login e cadastro
  const query = searchParams.toString() ? `?${searchParams.toString()}` : ""

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setErro(null)

    if (cadastro) {
      if (nome.trim().length < 2) return setErro("Informe seu nome")
      if (!senhaAtendeObrigatorias(senha)) return setErro("A senha não atende aos requisitos obrigatórios")
      if (senha !== confirmacao) return setErro("As senhas não conferem")
    }

    setEnviando(true)
    const mensagem = cadastro ? await registrar(nome.trim(), email.trim(), senha) : await login(email.trim(), senha)
    setEnviando(false)

    if (mensagem) setErro(mensagem)
    else router.push(destino)
  }

  return (
    <div className="min-h-[calc(100vh-8rem)] gradient-bg flex items-center justify-center px-4 py-10">
      <Card className="w-full max-w-md border-0 shadow-lg theme-transition">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 h-12 w-12 rounded-lg bg-primary flex items-center justify-center orange-glow">
            <Flame className="h-6 w-6 text-primary-foreground" />
          </div>
          <CardTitle className="text-2xl">{cadastro ? "Criar conta" : "Entrar"}</CardTitle>
          <CardDescription>
            {cadastro
              ? "Crie sua conta para salvar suas listas de compras."
              : "Acesse sua conta para salvar sua lista de compras."}
          </CardDescription>
        </CardHeader>

        <form onSubmit={handleSubmit} noValidate>
          <CardContent className="space-y-4">
            {motivo && (
              <Alert>
                <AlertDescription>{motivo}</AlertDescription>
              </Alert>
            )}

            {cadastro && (
              <div className="space-y-2">
                <Label htmlFor="nome">Nome</Label>
                <Input
                  id="nome"
                  autoComplete="name"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  maxLength={100}
                  required
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="email">E-mail</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="senha">Senha</Label>
              <div className="relative">
                <Input
                  id="senha"
                  type={mostrarSenha ? "text" : "password"}
                  autoComplete={cadastro ? "new-password" : "current-password"}
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  className="pr-10"
                  required
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="absolute right-0 top-0 h-full px-3 hover:bg-transparent"
                  onClick={() => setMostrarSenha((v) => !v)}
                >
                  {mostrarSenha ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  <span className="sr-only">{mostrarSenha ? "Esconder senha" : "Mostrar senha"}</span>
                </Button>
              </div>
            </div>

            {cadastro && (
              <>
                {/* logo abaixo da senha, para não ficar escondido depois das recomendações */}
                <div className="space-y-2">
                  <Label htmlFor="confirmacao">Confirmar senha</Label>
                  <Input
                    id="confirmacao"
                    type={mostrarSenha ? "text" : "password"}
                    autoComplete="new-password"
                    placeholder="Digite a senha novamente"
                    value={confirmacao}
                    onChange={(e) => setConfirmacao(e.target.value)}
                    aria-invalid={confirmacao.length > 0 && senha !== confirmacao}
                    className={cn(confirmacao && senha !== confirmacao && "border-destructive focus-visible:ring-destructive")}
                    required
                  />
                  {confirmacao &&
                    (senha === confirmacao ? (
                      <p className="flex items-center gap-1 text-xs text-[hsl(var(--best-price))]">
                        <Check className="h-3.5 w-3.5" />
                        As senhas conferem
                      </p>
                    ) : (
                      <p className="text-xs text-destructive">As senhas não conferem</p>
                    ))}
                </div>
                <PasswordStrength senha={senha} nome={nome} email={email} />
              </>
            )}

            {erro && (
              <Alert variant="destructive">
                <AlertDescription>{erro}</AlertDescription>
              </Alert>
            )}
          </CardContent>

          <CardFooter className="flex flex-col gap-3">
            <Button type="submit" className="w-full" disabled={enviando}>
              {enviando && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {cadastro ? "Criar conta" : "Entrar"}
            </Button>
            <p className="text-sm text-muted-foreground">
              {cadastro ? "Já tem conta? " : "Não tem conta? "}
              <Link href={`${cadastro ? "/login" : "/cadastro"}${query}`} className="font-medium">
                {cadastro ? "Entrar" : "Criar conta"}
              </Link>
            </p>
            <Link href="/" className="text-xs text-muted-foreground hover:text-foreground">
              Continuar sem entrar
            </Link>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
