package org.scraper.dto;

import java.math.BigDecimal;

public class ScrapedProdutoDTO {
    public String nome;
    public String categoria;
    public String medida;      // unidade normalizada: kg, g, l, ml, un
    public float quantidade;   // quantidade da embalagem (ex.: 5 para "Arroz 5kg")
    public BigDecimal preco;

    public ScrapedProdutoDTO(String nome, String categoria, String medida, BigDecimal preco) {
        this(nome, categoria, medida, 0, preco);
    }

    public ScrapedProdutoDTO(String nome, String categoria, String medida, float quantidade, BigDecimal preco) {
        this.nome = nome;
        this.categoria = categoria;
        this.medida = medida;
        this.quantidade = quantidade;
        this.preco = preco;
    }
}
