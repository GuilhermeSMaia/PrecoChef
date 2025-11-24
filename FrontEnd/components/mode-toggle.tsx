"use client"

import { useState, useEffect } from "react"
import { Moon, Sun, Monitor } from "lucide-react"
import { useTheme } from "next-themes"
import { Button } from "@/components/ui/button"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"

export function ModeToggle() {
  const { setTheme, theme, resolvedTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  // Avoid hydration mismatch
  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) {
    return (
      <Button variant="ghost" size="icon" className="opacity-0">
        <Sun className="h-[1.2rem] w-[1.2rem]" />
      </Button>
    )
  }

  const currentTheme = theme === "system" ? resolvedTheme : theme

  return (
    <TooltipProvider>
      <Tooltip>
        <DropdownMenu>
          <TooltipTrigger asChild>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="relative overflow-hidden transition-all duration-300 hover-lift"
                aria-label="Alterar tema"
              >
                <Sun
                  className={`h-[1.2rem] w-[1.2rem] transition-all duration-500 ${
                    currentTheme === "light" ? "rotate-0 scale-100" : "rotate-90 scale-0"
                  }`}
                />
                <Moon
                  className={`absolute h-[1.2rem] w-[1.2rem] transition-all duration-500 ${
                    currentTheme === "dark" ? "rotate-0 scale-100" : "-rotate-90 scale-0"
                  }`}
                />
                <Monitor
                  className={`absolute h-[1.2rem] w-[1.2rem] transition-all duration-500 ${
                    theme === "system" ? "rotate-0 scale-100" : "rotate-90 scale-0"
                  }`}
                />
              </Button>
            </DropdownMenuTrigger>
          </TooltipTrigger>
          <TooltipContent side="bottom">
            <p>Alterar tema</p>
          </TooltipContent>
          <DropdownMenuContent align="end" className="animate-scale-in">
            <DropdownMenuItem onClick={() => setTheme("light")} className="flex items-center gap-2 cursor-pointer">
              <Sun className="h-4 w-4" />
              <span>Claro</span>
              {currentTheme === "light" && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary"></span>}
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setTheme("dark")} className="flex items-center gap-2 cursor-pointer">
              <Moon className="h-4 w-4" />
              <span>Escuro</span>
              {currentTheme === "dark" && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary"></span>}
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setTheme("system")} className="flex items-center gap-2 cursor-pointer">
              <Monitor className="h-4 w-4" />
              <span>Sistema</span>
              {theme === "system" && <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary"></span>}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </Tooltip>
    </TooltipProvider>
  )
}
