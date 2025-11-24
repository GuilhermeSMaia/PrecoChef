import type { Market, Product, Price } from "./types"

export const mockMarkets: Market[] = [
  {
    id: "m1",
    name: "Mercado Central",
    address: "Rua das Flores, 123 - Centro",
  },
  {
    id: "m2",
    name: "Super Economia",
    address: "Av. Principal, 456 - Bairro Novo",
  },
  {
    id: "m3",
    name: "Mercadinho do Bairro",
    address: "Rua da Paz, 789 - Vila Esperança",
  },
]

export const mockProducts: Product[] = [
  {
    id: "p1",
    name: "Arroz Integral",
    category: "Alimentos",
    unit: "kg",
  },
  {
    id: "p2",
    name: "Feijão Carioca",
    category: "Alimentos",
    unit: "kg",
  },
  {
    id: "p3",
    name: "Leite Integral",
    category: "Laticínios",
    unit: "L",
  },
  {
    id: "p4",
    name: "Café em Pó",
    category: "Bebidas",
    unit: "kg",
  },
  {
    id: "p5",
    name: "Açúcar Refinado",
    category: "Alimentos",
    unit: "kg",
  },
  {
    id: "p6",
    name: "Óleo de Soja",
    category: "Alimentos",
    unit: "L",
  },
]

export const mockPrices: Price[] = [
  // Arroz Integral
  { id: "pr1", product_id: "p1", market_id: "m1", price: 8.99, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr2", product_id: "p1", market_id: "m2", price: 9.29, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr3", product_id: "p1", market_id: "m3", price: 8.79, last_updated: "2024-01-15T10:00:00Z" },

  // Feijão Carioca
  { id: "pr4", product_id: "p2", market_id: "m1", price: 7.49, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr5", product_id: "p2", market_id: "m2", price: 6.99, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr6", product_id: "p2", market_id: "m3", price: 7.29, last_updated: "2024-01-15T10:00:00Z" },

  // Leite Integral
  { id: "pr7", product_id: "p3", market_id: "m1", price: 4.99, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr8", product_id: "p3", market_id: "m2", price: 5.29, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr9", product_id: "p3", market_id: "m3", price: 4.79, last_updated: "2024-01-15T10:00:00Z" },

  // Café em Pó
  { id: "pr10", product_id: "p4", market_id: "m1", price: 12.99, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr11", product_id: "p4", market_id: "m2", price: 11.89, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr12", product_id: "p4", market_id: "m3", price: 13.49, last_updated: "2024-01-15T10:00:00Z" },

  // Açúcar Refinado
  { id: "pr13", product_id: "p5", market_id: "m1", price: 3.99, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr14", product_id: "p5", market_id: "m2", price: 3.79, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr15", product_id: "p5", market_id: "m3", price: 4.19, last_updated: "2024-01-15T10:00:00Z" },

  // Óleo de Soja
  { id: "pr16", product_id: "p6", market_id: "m1", price: 6.49, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr17", product_id: "p6", market_id: "m2", price: 5.99, last_updated: "2024-01-15T10:00:00Z" },
  { id: "pr18", product_id: "p6", market_id: "m3", price: 6.79, last_updated: "2024-01-15T10:00:00Z" },
]
