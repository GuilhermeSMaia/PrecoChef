"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Palette, Check } from "lucide-react"
import { useToast } from "@/components/ui/use-toast"

const orangeThemes = [
  {
    id: "orange-light",
    name: "Laranja Claro",
    description: "Tom mais claro e vibrante",
    color: "hsl(33, 100%, 50%)",
    className: "orange-light",
  },
  {
    id: "orange-medium",
    name: "Laranja Médio",
    description: "Tom equilibrado (padrão)",
    color: "hsl(24, 95%, 53%)",
    className: "orange-medium",
  },
  {
    id: "orange-deep",
    name: "Laranja Profundo",
    description: "Tom mais escuro e elegante",
    color: "hsl(16, 85%, 55%)",
    className: "orange-deep",
  },
  {
    id: "orange-red",
    name: "Laranja Avermelhado",
    description: "Tom mais quente e intenso",
    color: "hsl(14, 100%, 57%)",
    className: "orange-red",
  },
]

export function ColorCustomizer() {
  const [selectedTheme, setSelectedTheme] = useState("orange-medium")
  const [isOpen, setIsOpen] = useState(false)
  const { toast } = useToast()

  useEffect(() => {
    // Load saved theme preference
    const savedTheme = localStorage.getItem("orange-theme") || "orange-medium"
    setSelectedTheme(savedTheme)
    applyTheme(savedTheme)
  }, [])

  const applyTheme = (themeId: string) => {
    const theme = orangeThemes.find((t) => t.id === themeId)
    if (!theme) return

    // Remove all orange theme classes
    orangeThemes.forEach((t) => {
      document.documentElement.classList.remove(t.className)
    })

    // Apply selected theme class
    if (theme.className !== "orange-medium") {
      document.documentElement.classList.add(theme.className)
    }

    // Save preference
    localStorage.setItem("orange-theme", themeId)
  }

  const handleThemeChange = (themeId: string) => {
    setSelectedTheme(themeId)
    applyTheme(themeId)
    setIsOpen(false)

    const theme = orangeThemes.find((t) => t.id === themeId)
    toast({
      title: "Tema alterado",
      description: `Tema ${theme?.name} aplicado com sucesso.`,
    })
  }

  const selectedThemeData = orangeThemes.find((t) => t.id === selectedTheme)

  return (
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger asChild>
        <Button variant="outline" size="icon" className="relative hover-lift theme-transition">
          <Palette className="h-4 w-4" />
          <div
            className="absolute -bottom-1 -right-1 w-3 h-3 rounded-full border border-background"
            style={{ backgroundColor: selectedThemeData?.color }}
          />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="end">
        <Card className="border-0 shadow-lg">
          <CardHeader className="pb-3">
            <CardTitle className="text-lg">Personalizar Cores</CardTitle>
            <CardDescription>Escolha a intensidade do laranja que mais combina com você</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {orangeThemes.map((theme) => (
              <div
                key={theme.id}
                className={`flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all hover:bg-accent ${
                  selectedTheme === theme.id ? "bg-accent border border-primary" : "border border-transparent"
                }`}
                onClick={() => handleThemeChange(theme.id)}
              >
                <div
                  className="w-8 h-8 rounded-full border-2 border-border flex items-center justify-center"
                  style={{ backgroundColor: theme.color }}
                >
                  {selectedTheme === theme.id && <Check className="h-4 w-4 text-white" />}
                </div>
                <div className="flex-1">
                  <p className="font-medium">{theme.name}</p>
                  <p className="text-sm text-muted-foreground">{theme.description}</p>
                </div>
              </div>
            ))}

            <div className="pt-3 border-t">
              <p className="text-xs text-muted-foreground">
                As cores são salvas automaticamente e aplicadas em toda a plataforma.
              </p>
            </div>
          </CardContent>
        </Card>
      </PopoverContent>
    </Popover>
  )
}
