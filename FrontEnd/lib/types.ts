export interface Market {
  id: string
  name: string
}

export interface Product {
  id: string
  name: string
  category: string
  unit: string
}

export interface Price {
  id: string
  productId: string
  marketId: string
  price: number
  lastUpdated: string
}

export interface ProductWithPrices extends Product {
  prices: Price[]
  lowestPrice?: Price
}

export interface ShoppingItem {
  product: Product
  price: Price
  market: Market
  quantity: number
}