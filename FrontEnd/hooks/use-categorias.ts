"use client"

import { useEffect, useState } from "react"

export interface CategoriaComContagem {
  nome: string
  produtos: number // quantos produtos têm essa categoria
}

// categorias cadastradas no backend (GET /categorias), para filtros e formulários ficarem sempre atualizados
export function useCategorias() {
  const [lista, setLista] = useState<CategoriaComContagem[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(false)

  useEffect(() => {
    const controller = new AbortController()

    fetch("http://localhost:8080/categorias", { signal: controller.signal })
      .then((response) => {
        if (!response.ok) throw new Error("Erro ao buscar categorias")
        return response.json() as Promise<CategoriaComContagem[]>
      })
      .then((data) => {
        setLista(data)
        setCarregando(false)
      })
      .catch((error) => {
        if (controller.signal.aborted) return
        console.error("Erro ao carregar categorias:", error)
        setErro(true)
        setCarregando(false)
      })

    return () => controller.abort()
  }, [])

  // categorias: só os nomes; lista: nomes com a contagem de produtos
  return { categorias: lista.map((c) => c.nome), lista, carregando, erro }
}
