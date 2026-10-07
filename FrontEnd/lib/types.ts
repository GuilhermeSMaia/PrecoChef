export interface Mercado {
  id: string
  nome: string
}

export interface Produto {
  id: string
  produtoname: string
  categoria: string
  medida: string
}

export interface Preco {
  id: string
  produto_id: string
  mercado: string
  preco: number
  ultimaAtualizacao: string
}

export interface ProdutoWithPrices extends Produto {
  precos: (Preco & { mercado: string })[]
  menorPreco?: Preco & { mercado: string }
}

export interface ShoppingItem {
  produto: Produto
  preco: Preco
  mercado: Mercado
  quantidade: number
}

// resposta de GET /produtos/paginado
export interface PaginaProdutos {
  itens: ProdutoWithPrices[]
  pagina: number // começa em 0
  tamanho: number
  total: number
  totalPaginas: number
}
