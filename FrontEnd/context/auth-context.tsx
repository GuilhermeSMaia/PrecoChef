"use client"

import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react"
import { apiFetch, gravarToken, lerToken, mensagemDeErro } from "@/lib/api"

export type Role = "user" | "admin"

export interface Usuario {
  id: number
  nome: string
  email: string
  role: Role
}

type AuthContextType = {
  usuario: Usuario | null
  // true enquanto confere, ao abrir o site, se o token salvo ainda vale
  carregando: boolean
  isAdmin: boolean
  // devolvem uma mensagem de erro, ou null em caso de sucesso
  login: (email: string, senha: string) => Promise<string | null>
  registrar: (nome: string, email: string, senha: string) => Promise<string | null>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [carregando, setCarregando] = useState(true)

  // valida o token salvo (pode ter expirado ou a role pode ter mudado)
  useEffect(() => {
    if (!lerToken()) {
      setCarregando(false)
      return
    }
    apiFetch("/auth/me")
      .then(async (response) => {
        if (response.ok) setUsuario(await response.json())
        else if (response.status === 401) gravarToken(null)
      })
      .catch((error) => console.error("Erro ao validar login:", error))
      .finally(() => setCarregando(false))
  }, [])

  const entrar = useCallback(async (caminho: string, corpo: object, erroPadrao: string) => {
    try {
      const response = await apiFetch(caminho, { method: "POST", body: JSON.stringify(corpo) })
      if (!response.ok) return mensagemDeErro(response, erroPadrao)
      const data: { token: string; usuario: Usuario } = await response.json()
      gravarToken(data.token)
      setUsuario(data.usuario)
      return null
    } catch {
      return "Não foi possível conectar ao servidor"
    }
  }, [])

  const login = useCallback(
    (email: string, senha: string) => entrar("/auth/login", { email, senha }, "Não foi possível entrar"),
    [entrar],
  )

  const registrar = useCallback(
    (nome: string, email: string, senha: string) =>
      entrar("/auth/registrar", { nome, email, senha }, "Não foi possível criar a conta"),
    [entrar],
  )

  const logout = useCallback(() => {
    gravarToken(null)
    setUsuario(null)
  }, [])

  return (
    <AuthContext.Provider
      value={{ usuario, carregando, isAdmin: usuario?.role === "admin", login, registrar, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error("useAuth deve ser usado dentro de um AuthProvider")
  }
  return context
}
