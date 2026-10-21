"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { ShoppingCart, Menu, X, Flame, Settings, Store, Home, LogIn, LogOut } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet"
import { useState } from "react"
import { useShoppingList } from "@/context/shopping-list-context"
import { ModeToggle } from "./mode-toggle"
import { ColorCustomizer } from "./color-customizer"
import { Badge } from "@/components/ui/badge"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useAuth } from "@/context/auth-context"

const navItems = [
  { name: "Home", path: "/", icon: Home },
  { name: "Produtos", path: "/produtos", icon: Flame },
  { name: "Comparador", path: "/comparador", icon: Menu },
  { name: "Mercados", path: "/mercados", icon: Store },
  { name: "Lista de Compras", path: "/lista", icon: ShoppingCart },
  { name: "Admin", path: "/admin", icon: Settings, soAdmin: true },
]

export function Navbar() {
  const pathname = usePathname()
  const [open, setOpen] = useState(false)
  const { items, getTotalItems } = useShoppingList()
  const totalItems = getTotalItems()
  const { usuario, isAdmin, carregando, logout } = useAuth()
  // o item "Admin" só aparece para administradores
  const itensVisiveis = navItems.filter((item) => !item.soAdmin || isAdmin)

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
                  PreçoChef
                </Link>
                <Button variant="ghost" size="icon" onClick={() => setOpen(false)}>
                  <X className="h-5 w-5" />
                </Button>
              </div>
              <nav className="flex flex-col gap-2">
                {itensVisiveis.map((item) => {
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
            PreçoChef
          </Link>
        </div>

        <nav className="hidden lg:flex items-center gap-1">
          {itensVisiveis.map((item) => {
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
          {!carregando &&
            (usuario ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" className="gap-2 px-2 hover-lift theme-transition">
                    <span className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center text-sm font-semibold">
                      {usuario.nome.trim().charAt(0).toUpperCase()}
                    </span>
                    <span className="hidden sm:inline max-w-[120px] truncate">{usuario.nome.split(" ")[0]}</span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-56">
                  <DropdownMenuLabel className="font-normal">
                    <p className="font-medium truncate">{usuario.nome}</p>
                    <p className="text-xs text-muted-foreground truncate">{usuario.email}</p>
                    {isAdmin && <Badge className="mt-1.5">Administrador</Badge>}
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem asChild>
                    <Link href="/lista">
                      <ShoppingCart className="mr-2 h-4 w-4" />
                      Minha lista
                    </Link>
                  </DropdownMenuItem>
                  {isAdmin && (
                    <DropdownMenuItem asChild>
                      <Link href="/admin">
                        <Settings className="mr-2 h-4 w-4" />
                        Painel admin
                      </Link>
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={logout} className="text-destructive focus:text-destructive">
                    <LogOut className="mr-2 h-4 w-4" />
                    Sair
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <Button asChild variant="outline" size="sm" className="gap-2 hover-lift theme-transition">
                <Link href={`/login?redirect=${encodeURIComponent(pathname)}`}>
                  <LogIn className="h-4 w-4" />
                  Entrar
                </Link>
              </Button>
            ))}
        </div>
      </div>
    </header>
  )
}
