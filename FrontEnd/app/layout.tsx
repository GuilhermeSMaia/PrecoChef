import type React from "react"
import type { Metadata } from "next"
import { Inter } from "next/font/google"
import "./globals.css"
import { Navbar } from "@/components/navbar"
import { ThemeProvider } from "@/components/theme-provider"
import { Toaster } from "@/components/ui/toaster"
import { ShoppingListProvider } from "@/context/shopping-list-context"

const inter = Inter({ subsets: ["latin"] })

export const metadata: Metadata = {
  title: "Comparador de Preços",
  description: "Compare preços entre mercados e economize nas suas compras",
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#ffffff" },
    { media: "(prefers-color-scheme: dark)", color: "#0f172a" },
  ],
    generator: 'v0.dev'
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body className={`${inter.className} antialiased`}>
        <ThemeProvider>
          <ShoppingListProvider>
            <div className="min-h-screen flex flex-col">
              <Navbar />
              <main className="flex-1">{children}</main>
              <footer className="py-6 border-t theme-transition">
                <div className="container mx-auto px-4 text-center text-sm text-muted-foreground">
                  © {new Date().getFullYear()} Comparador de Preços
                </div>
              </footer>
            </div>
            <Toaster />
          </ShoppingListProvider>
        </ThemeProvider>
      </body>
    </html>
  )
}
