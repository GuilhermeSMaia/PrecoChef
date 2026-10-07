package org.scraper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.scraper.dto.ScrapedProdutoDTO;

class AtacadaoScraperTest {

    // formato da resposta de /api/io/_v/api/intelligent-search/product_search/category-1/{departamento}
    private static final String RESPOSTA = """
            {"products":[
              {"productId":"101","productName":"Arroz Branco Tio João Tipo 1 5kg","brand":"Tio João",
               "categories":["/Mercearia/Arroz e Feijão/Arroz/","/Mercearia/Arroz e Feijão/","/Mercearia/"],
               "items":[{"itemId":"1","measurementUnit":"un",
                 "sellers":[{"sellerId":"1","commertialOffer":{"Price":27.9,"ListPrice":27.9,"AvailableQuantity":10000}}]}]},
              {"productId":"102","productName":"Patinho Bovino Resfriado Kg","brand":"",
               "categories":["/Carnes, Aves e Peixes/Bovinos/","/Carnes, Aves e Peixes/"],
               "items":[{"sellers":[{"commertialOffer":{"Price":39.99,"AvailableQuantity":50}}]}]},
              {"productId":"103","productName":"Refrigerante Coca-Cola 2L","brand":"Coca-Cola",
               "categories":["/Bebidas/Refrigerantes/","/Bebidas/"],
               "items":[{"sellers":[{"commertialOffer":{"Price":0,"AvailableQuantity":0}}]}]},
              {"productId":"104","productName":"Cerveja Skol Lata 12x350ml","brand":"Skol",
               "categories":["/Bebidas/Cervejas/","/Bebidas/"],
               "items":[{"sellers":[{"commertialOffer":{"Price":37.5,"AvailableQuantity":3}}]}]}
            ],"recordsFiltered":4}
            """;

    @Test
    void leProdutosDaApi() {
        AtacadaoScraper.Resposta r = AtacadaoScraper.parseResposta(RESPOSTA);

        assertEquals(4, r.quantidadeNaPagina());
        assertEquals(3, r.produtos().size()); // o produto sem preço/estoque é descartado

        ScrapedProdutoDTO arroz = r.produtos().get("101");
        assertEquals("Arroz Branco Tio João Tipo 1 5kg", arroz.nome);
        assertEquals("Mercearia", arroz.categoria); // departamento vira a categoria
        assertEquals("Tio João", arroz.marca);
        assertEquals(new BigDecimal("27.90"), arroz.preco);
        assertEquals("kg", arroz.medida);
        assertEquals(5f, arroz.quantidade);
    }

    @Test
    void produtoVendidoPorKgSemMarca() {
        ScrapedProdutoDTO patinho = AtacadaoScraper.parseResposta(RESPOSTA).produtos().get("102");

        assertEquals("Carnes, Aves e Peixes", patinho.categoria);
        assertEquals("kg", patinho.medida);
        assertEquals(1f, patinho.quantidade);
        assertEquals(new BigDecimal("39.99"), patinho.preco);
        assertNull(patinho.marca);
    }

    @Test
    void embalagemComVariasUnidades() {
        ScrapedProdutoDTO cerveja = AtacadaoScraper.parseResposta(RESPOSTA).produtos().get("104");

        assertEquals("ml", cerveja.medida);
        assertEquals(350f, cerveja.quantidade);
    }

    @Test
    void respostaSemProdutos() {
        AtacadaoScraper.Resposta r = AtacadaoScraper.parseResposta("{\"products\":[],\"recordsFiltered\":0}");
        assertEquals(0, r.quantidadeNaPagina());
        assertEquals(0, r.produtos().size());
    }
}
