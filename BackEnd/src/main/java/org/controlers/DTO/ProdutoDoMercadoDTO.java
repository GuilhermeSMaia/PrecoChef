package org.controlers.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

// um produto com o preço dele em um mercado específico (tabela do pop-up de detalhes do mercado)
public class ProdutoDoMercadoDTO {
    public Long produtoId;
    public String produto;
    public String categoria;
    public BigDecimal preco;
    public LocalDate ultimaAtualizacao;

    public ProdutoDoMercadoDTO(Long produtoId, String produto, String categoria, BigDecimal preco, LocalDate ultimaAtualizacao) {
        this.produtoId = produtoId;
        this.produto = produto;
        this.categoria = categoria;
        this.preco = preco;
        this.ultimaAtualizacao = ultimaAtualizacao;
    }
}
