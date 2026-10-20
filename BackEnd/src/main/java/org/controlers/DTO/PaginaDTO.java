package org.controlers.DTO;

import java.util.List;

public class PaginaDTO<T> {
    public List<T> itens;
    public int pagina;        // começa em 0
    public int tamanho;
    public long total;
    public int totalPaginas;

    public PaginaDTO(List<T> itens, int pagina, int tamanho, long total) {
        this.itens = itens;
        this.pagina = pagina;
        this.tamanho = tamanho;
        this.total = total;
        this.totalPaginas = (int) Math.ceil((double) total / tamanho);
    }
}
