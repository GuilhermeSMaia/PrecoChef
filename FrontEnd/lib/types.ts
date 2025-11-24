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
