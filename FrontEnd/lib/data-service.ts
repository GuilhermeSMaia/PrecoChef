import type { Market, Product, Price, ProductWithPrices } from "./types"
import { mockMarkets, mockProducts, mockPrices } from "./mock-data"

export class DataService {
  static async getMarkets(): Promise<Market[]> {
    // Simular delay de API
    await new Promise((resolve) => setTimeout(resolve, 100))
    return mockMarkets
  }

  static async getProducts(): Promise<Product[]> {
    await new Promise((resolve) => setTimeout(resolve, 100))
    return mockProducts
  }

  static async getPrices(): Promise<Price[]> {
    await new Promise((resolve) => setTimeout(resolve, 100))
    return mockPrices
  }

  static async getProductsWithPrices(filters?: {
    search?: string
    category?: string
    sortBy?: string
  }): Promise<ProductWithPrices[]> {
    const [products, prices, markets] = await Promise.all([this.getProducts(), this.getPrices(), this.getMarkets()])

    let filteredProducts = products

    // Aplicar filtros
    if (filters?.search) {
      filteredProducts = filteredProducts.filter((product) =>
        product.name.toLowerCase().includes(filters.search!.toLowerCase()),
      )
    }

    if (filters?.category) {
      filteredProducts = filteredProducts.filter((product) => product.category === filters.category)
    }

    // Combinar produtos com preços e mercados
    const productsWithPrices: ProductWithPrices[] = filteredProducts.map((product) => {
      const productPrices = prices
        .filter((price) => price.product_id === product.id)
        .map((price) => ({
          ...price,
          market: markets.find((market) => market.id === price.market_id)!,
        }))
        .sort((a, b) => a.price - b.price)

      return {
        ...product,
        prices: productPrices,
        lowestPrice: productPrices[0],
      }
    })

    // Aplicar ordenação
    if (filters?.sortBy === "price-asc") {
      productsWithPrices.sort((a, b) => (a.lowestPrice?.price || 0) - (b.lowestPrice?.price || 0))
    } else if (filters?.sortBy === "price-desc") {
      productsWithPrices.sort((a, b) => (b.lowestPrice?.price || 0) - (a.lowestPrice?.price || 0))
    } else if (filters?.sortBy === "name-asc") {
      productsWithPrices.sort((a, b) => a.name.localeCompare(b.name))
    }

    return productsWithPrices
  }

  static async getProductComparison(productId: string): Promise<ProductWithPrices | null> {
    const [products, prices, markets] = await Promise.all([this.getProducts(), this.getPrices(), this.getMarkets()])

    const product = products.find((p) => p.id === productId)
    if (!product) return null

    const productPrices = prices
      .filter((price) => price.product_id === product.id)
      .map((price) => ({
        ...price,
        market: markets.find((market) => market.id === price.market_id)!,
      }))
      .sort((a, b) => a.price - b.price)

    return {
      ...product,
      prices: productPrices,
      lowestPrice: productPrices[0],
    }
  }

  static async searchProducts(query: string): Promise<Product[]> {
    const products = await this.getProducts()
    return products.filter((product) => product.name.toLowerCase().includes(query.toLowerCase()))
  }

  static getCategories(): string[] {
    return ["Alimentos", "Laticínios", "Bebidas", "Limpeza", "Higiene"]
  }
}
