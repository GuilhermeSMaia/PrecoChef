import type { ShoppingItem } from "@/lib/types"

const MARGEM = 20 // mm
const LARGURA_PAGINA = 210 // A4
const ALTURA_PAGINA = 297
const LADO_CHECKBOX = 4.5
const ALTURA_LINHA = 6

// abre em outra aba um PDF da lista, agrupado por mercado, com uma caixinha em cada item para riscar à caneta
export async function exportarListaPdf(titulo: string, items: ShoppingItem[]) {
  // a aba precisa ser aberta ainda dentro do clique, senão o navegador bloqueia como pop-up
  const aba = window.open("", "_blank")

  const { jsPDF } = await import("jspdf")
  const doc = new jsPDF({ unit: "mm", format: "a4" })

  const larguraUtil = LARGURA_PAGINA - MARGEM * 2
  let y = MARGEM

  const garantirEspaco = (altura: number) => {
    if (y + altura > ALTURA_PAGINA - MARGEM) {
      doc.addPage()
      y = MARGEM
    }
  }

  doc.setFont("helvetica", "bold")
  doc.setFontSize(18)
  doc.text(titulo || "Lista de Compras", MARGEM, y + 6)
  y += 10

  doc.setFont("helvetica", "normal")
  doc.setFontSize(10)
  doc.setTextColor(110)
  doc.text(new Date().toLocaleDateString("pt-BR"), MARGEM, y + 4)
  doc.setTextColor(0)
  y += 12

  const porMercado = new Map<string, ShoppingItem[]>()
  items.forEach((item) => {
    const nome = item.mercado.nome
    porMercado.set(nome, [...(porMercado.get(nome) ?? []), item])
  })

  porMercado.forEach((itensDoMercado, mercado) => {
    // não deixa o nome do mercado sozinho no fim da página
    garantirEspaco(14 + ALTURA_LINHA)

    doc.setFont("helvetica", "bold")
    doc.setFontSize(14)
    doc.text(mercado, MARGEM, y + 5)
    y += 8
    doc.setLineWidth(0.4)
    doc.line(MARGEM, y, MARGEM + larguraUtil, y)
    y += 5

    doc.setFont("helvetica", "normal")
    doc.setFontSize(11)

    itensDoMercado.forEach((item) => {
      const quantidade = `${item.quantidade} ${item.quantidade === 1 ? "unidade" : "unidades"}`
      const larguraQuantidade = doc.getTextWidth(quantidade)
      const xTexto = MARGEM + LADO_CHECKBOX + 4
      // nomes longos quebram em várias linhas sem invadir a coluna da quantidade
      const linhasNome: string[] = doc.splitTextToSize(
        item.produto.produtoname,
        larguraUtil - (xTexto - MARGEM) - larguraQuantidade - 6,
      )
      const alturaItem = linhasNome.length * ALTURA_LINHA + 2

      garantirEspaco(alturaItem)

      doc.setLineWidth(0.3)
      doc.rect(MARGEM, y, LADO_CHECKBOX, LADO_CHECKBOX)
      doc.text(linhasNome, xTexto, y + LADO_CHECKBOX - 0.6)
      doc.text(quantidade, MARGEM + larguraUtil, y + LADO_CHECKBOX - 0.6, { align: "right" })

      y += alturaItem
    })

    y += 6
  })

  const url = doc.output("bloburl").toString()
  if (aba) {
    aba.location.href = url
  } else {
    // pop-up bloqueado: baixa o arquivo
    doc.save(`${(titulo || "lista-de-compras").trim()}.pdf`)
  }
}
