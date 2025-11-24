import { ShoppingList } from "@/components/shopping-list"

export default function ListaPage() {
  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Lista de Compras</h1>
      <ShoppingList />
    </div>
  )
}
