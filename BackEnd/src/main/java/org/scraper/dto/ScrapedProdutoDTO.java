package org.scraper.dto;

import java.math.BigDecimal;

public class ScrapedProdutoDTO {
    public String nome;
    public String categoria;
    public String medida;
    public BigDecimal preco;

    public ScrapedProdutoDTO(String nome, String categoria, String medida, BigDecimal preco) {
        this.nome = nome;
        this.categoria = categoria;
        this.medida = medida;
        this.preco = preco;
    }
}