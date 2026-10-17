import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

// formata a data do backend (LocalDate "2026-10-16") como "16/10/2026".
// new Date("2026-10-16") seria meia-noite em UTC e, no fuso de Porto Velho (UTC-4), viraria o dia anterior
export function formatarData(data?: string | null): string {
  if (!data) return ""
  const [, ano, mes, dia] = /^(\d{4})-(\d{2})-(\d{2})/.exec(data) ?? []
  if (ano) return `${dia}/${mes}/${ano}`
  const d = new Date(data)
  return isNaN(d.getTime()) ? data : d.toLocaleDateString("pt-BR")
}
