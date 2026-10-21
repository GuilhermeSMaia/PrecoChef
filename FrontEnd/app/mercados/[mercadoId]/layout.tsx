import type { ReactNode } from "react"
import { AdminOnly } from "@/components/admin-only"

// todas as páginas desta pasta são só para admin
export default function Layout({ children }: { children: ReactNode }) {
  return <AdminOnly>{children}</AdminOnly>
}
