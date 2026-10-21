"use client"

import { Check, Circle, X } from "lucide-react"
import { cn } from "@/lib/utils"

interface Regra {
  texto: string
  ok: boolean
  // obrigatórias são as mesmas que o backend exige; as demais são recomendações
  obrigatoria: boolean
}

// letras incluindo acentuadas (À-ÿ, sem × e ÷)
const LETRA = /[A-Za-zÀ-ÖØ-öø-ÿ]/
const MAIUSCULA = /[A-ZÀ-ÖØ-Þ]/
const MINUSCULA = /[a-zß-öø-ÿ]/
const SIMBOLO = /[^A-Za-zÀ-ÖØ-öø-ÿ0-9\s]/

export function regrasDaSenha(senha: string, nome = "", email = ""): Regra[] {
  const minusculo = senha.toLowerCase()
  const usuarioEmail = email.split("@")[0]?.toLowerCase() ?? ""
  const partesNome = nome
    .toLowerCase()
    .split(/\s+/)
    .filter((p) => p.length >= 3)

  return [
    { texto: "Pelo menos 8 caracteres", ok: senha.length >= 8, obrigatoria: true },
    { texto: "Pelo menos uma letra", ok: LETRA.test(senha), obrigatoria: true },
    { texto: "Pelo menos um número", ok: /\d/.test(senha), obrigatoria: true },
    { texto: "Letras maiúsculas e minúsculas", ok: MAIUSCULA.test(senha) && MINUSCULA.test(senha), obrigatoria: false },
    { texto: "Um símbolo (ex.: ! @ # $ %)", ok: SIMBOLO.test(senha), obrigatoria: false },
    { texto: "12 caracteres ou mais", ok: senha.length >= 12, obrigatoria: false },
    {
      texto: "Não usar seu nome ou e-mail",
      ok:
        senha.length > 0 &&
        !partesNome.some((p) => minusculo.includes(p)) &&
        !(usuarioEmail.length >= 3 && minusculo.includes(usuarioEmail)),
      obrigatoria: false,
    },
  ]
}

export function senhaAtendeObrigatorias(senha: string) {
  return regrasDaSenha(senha).every((r) => !r.obrigatoria || r.ok) && new TextEncoder().encode(senha).length <= 72
}

const NIVEIS = [
  { rotulo: "Muito fraca", cor: "bg-destructive" },
  { rotulo: "Fraca", cor: "bg-orange-500" },
  { rotulo: "Razoável", cor: "bg-yellow-500" },
  { rotulo: "Boa", cor: "bg-lime-500" },
  { rotulo: "Forte", cor: "bg-[hsl(var(--best-price))]" },
]

interface PasswordStrengthProps {
  senha: string
  nome?: string
  email?: string
}

export function PasswordStrength({ senha, nome, email }: PasswordStrengthProps) {
  const regras = regrasDaSenha(senha, nome, email)
  const obrigatoriasOk = regras.every((r) => !r.obrigatoria || r.ok)
  const recomendacoesOk = regras.filter((r) => !r.obrigatoria && r.ok).length

  // sem as obrigatórias a senha nem é aceita; com elas, cada recomendação sobe um nível
  const nivel = !senha ? -1 : obrigatoriasOk ? Math.min(1 + recomendacoesOk, 4) : 0

  return (
    <div className="space-y-3 rounded-lg border bg-muted/30 p-3 text-sm theme-transition">
      <div className="space-y-1.5">
        <div className="flex gap-1">
          {NIVEIS.map((n, i) => (
            <div
              key={n.rotulo}
              className={cn("h-1.5 flex-1 rounded-full transition-colors", i <= nivel ? NIVEIS[nivel].cor : "bg-muted")}
            />
          ))}
        </div>
        <p className="text-xs text-muted-foreground">
          Força da senha: <span className="font-medium text-foreground">{nivel < 0 ? "—" : NIVEIS[nivel].rotulo}</span>
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        {[true, false].map((obrigatoria) => (
          <div key={String(obrigatoria)}>
            <p className="mb-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">
              {obrigatoria ? "Obrigatório" : "Recomendado"}
            </p>
            <ul className="space-y-1">
              {regras
                .filter((r) => r.obrigatoria === obrigatoria)
                .map((r) => (
                  <li key={r.texto} className={cn("flex items-center gap-1.5", r.ok ? "text-foreground" : "text-muted-foreground")}>
                    {r.ok ? (
                      <Check className="h-3.5 w-3.5 shrink-0 text-[hsl(var(--best-price))]" />
                    ) : obrigatoria && senha ? (
                      <X className="h-3.5 w-3.5 shrink-0 text-destructive" />
                    ) : (
                      <Circle className="h-3 w-3 shrink-0 opacity-50" />
                    )}
                    {r.texto}
                  </li>
                ))}
            </ul>
          </div>
        ))}
      </div>
    </div>
  )
}
