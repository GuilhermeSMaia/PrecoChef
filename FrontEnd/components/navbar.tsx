"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { ShoppingCart, Menu, X, Flame, Settings, Store } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet"
import { useState } from "react"
import { useShoppingList } from "@/context/shopping-list-context"
import { ModeToggle } from "./mode-toggle"
import { ColorCustomizer } from "./color-customizer"
import { Badge } from "@/components/ui/badge"

const navItems = [
  { name: "Home", path: "/" },
  { name: "Produtos", path: "/produtos" },
  { name: "Comparador", path: "/comparador" },
  { name: "Mercados", path: "/mercados", icon: Store },
  { name: "Lista de Compras", path: "/lista" },
  { name: "Admin", path: "/admin", icon: Settings },
]

export function Navbar() {
  const pathname = usePathname()
  const [open, setOpen] = useState(false)
  const { items, getTotalItems } = useShoppingList()
  const totalItems = getTotalItems()

  return (
    <header className="sticky top-0 z-50 w-full glass-effect border-b theme-transition">
      <div className="container flex h-16 items-center justify-between">
        <div className="flex items-center gap-2">
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild className="lg:hidden">
              <Button variant="ghost" size="icon" className="hover-lift theme-transition">
                <Menu className="h-5 w-5" />
                <span className="sr-only">Toggle menu</span>
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="w-[280px] sm:w-[320px] theme-transition">
              <div className="flex items-center justify-between mb-8">
                <Link href="/" className="flex items-center gap-2 font-bold text-xl" onClick={() => setOpen(false)}>
                  <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center theme-transition">
                    <Flame className="h-4 w-4 text-primary-foreground" />
                  </div>
                  ComparaPreços
                </Link>
                <Button variant="ghost" size="icon" onClick={() => setOpen(false)}>
                  <X className="h-5 w-5" />
                </Button>
              </div>
              <nav className="flex flex-col gap-2">
                {navItems.map((item) => {
                  const Icon = item.icon
                  return (
                    <Link
                      key={item.path}
                      href={item.path}
                      className={`text-lg px-4 py-3 rounded-lg transition-colors hover:bg-accent theme-transition flex items-center gap-2 ${
                        pathname === item.path ? "bg-accent font-medium text-primary" : ""
                      }`}
                      onClick={() => setOpen(false)}
                    >
                      {Icon && <Icon className="h-4 w-4" />}
                      {item.name}
                    </Link>
                  )
                })}
              </nav>
            </SheetContent>
          </Sheet>

          <Link href="/" className="flex items-center gap-2 font-bold text-xl hidden md:inline-flex">
            <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center theme-transition orange-glow">
              <Flame className="h-4 w-4 text-primary-foreground" />
            </div>
            ComparaPreços
          </Link>
        </div>

        <nav className="hidden lg:flex items-center gap-1">
          {navItems.map((item) => {
            const Icon = item.icon
            return (
              <Link
                key={item.path}
                href={item.path}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all hover:bg-accent hover-lift theme-transition flex items-center gap-2 ${
                  pathname === item.path ? "bg-accent text-primary" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {Icon && <Icon className="h-4 w-4" />}
                {item.name}
              </Link>
            )
          })}
        </nav>

        <div className="flex items-center gap-2">
          <ModeToggle />
          
        </div>
      </div>
    </header>
  )
}
