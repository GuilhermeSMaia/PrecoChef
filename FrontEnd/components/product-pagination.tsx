"use client"

import { ChevronLeft, ChevronRight } from "lucide-react"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
} from "@/components/ui/pagination"
import { cn } from "@/lib/utils"

interface ProductPaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

function getPageItems(page: number, totalPages: number): (number | "ellipsis")[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1)
  }

  const items: (number | "ellipsis")[] = [1]
  const start = Math.max(2, page - 1)
  const end = Math.min(totalPages - 1, page + 1)

  if (start > 2) items.push("ellipsis")
  for (let i = start; i <= end; i++) items.push(i)
  if (end < totalPages - 1) items.push("ellipsis")
  items.push(totalPages)

  return items
}

export function ProductPagination({ page, totalPages, onPageChange }: ProductPaginationProps) {
  if (totalPages <= 1) return null

  const goTo = (target: number) => (e: React.MouseEvent) => {
    e.preventDefault()
    if (target < 1 || target > totalPages || target === page) return
    onPageChange(target)
  }

  const isFirst = page === 1
  const isLast = page === totalPages

  return (
    <Pagination className="mt-8">
      <PaginationContent className="flex-wrap justify-center">
        <PaginationItem>
          <PaginationLink
            href="#"
            size="default"
            aria-label="Página anterior"
            aria-disabled={isFirst}
            onClick={goTo(page - 1)}
            className={cn("gap-1 pl-2.5", isFirst && "pointer-events-none opacity-50")}
          >
            <ChevronLeft className="h-4 w-4" />
            <span>Anterior</span>
          </PaginationLink>
        </PaginationItem>

        {getPageItems(page, totalPages).map((item, index) =>
          item === "ellipsis" ? (
            <PaginationItem key={`ellipsis-${index}`}>
              <PaginationEllipsis />
            </PaginationItem>
          ) : (
            <PaginationItem key={item}>
              <PaginationLink href="#" isActive={item === page} onClick={goTo(item)}>
                {item}
              </PaginationLink>
            </PaginationItem>
          ),
        )}

        <PaginationItem>
          <PaginationLink
            href="#"
            size="default"
            aria-label="Próxima página"
            aria-disabled={isLast}
            onClick={goTo(page + 1)}
            className={cn("gap-1 pr-2.5", isLast && "pointer-events-none opacity-50")}
          >
            <span>Próxima</span>
            <ChevronRight className="h-4 w-4" />
          </PaginationLink>
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  )
}
