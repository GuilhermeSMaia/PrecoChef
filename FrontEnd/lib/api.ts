export const API_URL = "http://localhost:8080"

const CHAVE_TOKEN = "authToken"

// o token fica no localStorage para o login sobreviver a recarregar a página
export function lerToken(): string | null {
  try {
    return localStorage.getItem(CHAVE_TOKEN)
  } catch {
    return null
  }
}

export function gravarToken(token: string | null) {
  try {
    if (token) localStorage.setItem(CHAVE_TOKEN, token)
    else localStorage.removeItem(CHAVE_TOKEN)
  } catch {
    // navegação privada / armazenamento bloqueado: o login vale só até recarregar
  }
}

// fetch para o backend que já envia o token do usuário logado (quando houver)
export function apiFetch(caminho: string, init: RequestInit = {}) {
  const headers = new Headers(init.headers)
  const token = lerToken()
  if (token) headers.set("Authorization", `Bearer ${token}`)
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json")
  return fetch(`${API_URL}${caminho}`, { ...init, headers })
}

// o backend responde erros de login/cadastro como { mensagem }
export async function mensagemDeErro(response: Response, padrao: string): Promise<string> {
  try {
    const data = await response.json()
    if (data && typeof data.mensagem === "string") return data.mensagem
  } catch {
    // corpo vazio ou não-JSON
  }
  if (response.status === 401) return "Sua sessão expirou. Entre novamente."
  if (response.status === 403) return "Você não tem permissão para esta ação."
  return padrao
}
