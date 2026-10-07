"use client"

import { ChevronLeft, ChevronRight } from "lucide-react"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
} from "@/components/ui/pagination"

interface ProductPaginationProps {
  pagina: number // começa em 0
  totalPaginas: number
  onPageChange: (pagina: number) => void
}

// números exibidos: primeira, última e até 2 vizinhas da atual; null = reticências
function paginasVisiveis(atual: number, total: number): (number | null)[] {
  const paginas: (number | null)[] = []
  for (let i = 0; i < total; i++) {
    if (i === 0 || i === total - 1 || Math.abs(i - atual) <= 2) {
      paginas.push(i)
    } else if (paginas[paginas.length - 1] !== null) {
      paginas.push(null)
    }
  }
  return paginas
}

export function ProductPagination({ pagina, totalPaginas, onPageChange }: ProductPaginationProps) {
  if (totalPaginas <= 1) return null

  const irPara = (destino: number) => (e: React.MouseEvent) => {
    e.preventDefault()
    if (destino >= 0 && destino < totalPaginas && destino !== pagina) onPageChange(destino)
  }

  const desabilitado = "pointer-events-none opacity-50"

  return (
    <Pagination className="mt-8">
      <PaginationContent className="flex-wrap justify-center">
        <PaginationItem>
          <PaginationLink
            href="#"
            size="default"
            aria-label="Página anterior"
            className={`gap-1 pl-2.5 ${pagina === 0 ? desabilitado : ""}`}
            onClick={irPara(pagina - 1)}
          >
            <ChevronLeft className="h-4 w-4" />
            <span className="hidden sm:inline">Anterior</span>
          </PaginationLink>
        </PaginationItem>

        {paginasVisiveis(pagina, totalPaginas).map((p, i) =>
          p === null ? (
            <PaginationItem key={`reticencias-${i}`}>
              <PaginationEllipsis />
            </PaginationItem>
          ) : (
            <PaginationItem key={p}>
              <PaginationLink href="#" isActive={p === pagina} onClick={irPara(p)}>
                {p + 1}
              </PaginationLink>
            </PaginationItem>
          ),
        )}

        <PaginationItem>
          <PaginationLink
            href="#"
            size="default"
            aria-label="Próxima página"
            className={`gap-1 pr-2.5 ${pagina >= totalPaginas - 1 ? desabilitado : ""}`}
            onClick={irPara(pagina + 1)}
          >
            <span className="hidden sm:inline">Próxima</span>
            <ChevronRight className="h-4 w-4" />
          </PaginationLink>
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  )
}
