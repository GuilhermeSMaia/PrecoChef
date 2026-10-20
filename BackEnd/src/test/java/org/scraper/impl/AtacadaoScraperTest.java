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
               "items":[{"itemId":"1001","ean":"7893500020110","measurementUnit":"un",
                 "sellers":[{"sellerId":"1","commertialOffer":{"Price":27.9,"AvailableQuantity":10000}}]}]},
              {"productId":"102","productName":"Patinho Bovino Resfriado Kg","brand":"",
               "categories":["/Carnes, Aves e Peixes/Bovinos/","/Carnes, Aves e Peixes/"],
               "items":[{"itemId":"1002","sellers":[{"sellerId":"1","commertialOffer":{"Price":39.99,"AvailableQuantity":50}}]}]},
              {"productId":"103","productName":"Refrigerante Coca-Cola 2L","brand":"Coca-Cola",
               "categories":["/Bebidas/Refrigerantes/","/Bebidas/"],
               "items":[]},
              {"productId":"104","productName":"Cerveja Skol Lata 12x350ml","brand":"Skol",
               "categories":["/Bebidas/Cervejas/","/Bebidas/"],
               "items":[{"itemId":"1004","sellers":[{"sellerId":"1","commertialOffer":{"Price":0,"AvailableQuantity":0}}]}]}
            ],"recordsFiltered":4}
            """;

    @Test
    void leCatalogoDaBusca() {
        AtacadaoScraper.PaginaBusca r = AtacadaoScraper.parseBusca(RESPOSTA);

        assertEquals(4, r.quantidadeNaPagina());
        assertEquals(3, r.produtos().size()); // o produto sem SKU é descartado

        AtacadaoScraper.ProdutoBusca arroz = r.produtos().get("101");
        assertEquals("1001", arroz.itemId());
        assertEquals("Mercearia", arroz.categoria()); // departamento vira a categoria
        assertEquals("Tio João", arroz.marca());
        assertEquals("7893500020110", arroz.ean());
    }

    @Test
    void ignoraPrecoEEstoqueDaBusca() {
        // o preço da busca é o genérico do site; disponibilidade e preço vêm da simulação na filial
        AtacadaoScraper.ProdutoBusca cerveja = AtacadaoScraper.parseBusca(RESPOSTA).produtos().get("104");
        assertEquals("1004", cerveja.itemId());
    }

    @Test
    void montaDtoComPrecoDaFilial() {
        ScrapedProdutoDTO arroz = AtacadaoScraper.parseBusca(RESPOSTA).produtos().get("101").paraDto(new BigDecimal("25.49"));

        assertEquals("Arroz Branco Tio João Tipo 1 5kg", arroz.nome);
        assertEquals(new BigDecimal("25.49"), arroz.preco);
        assertEquals("kg", arroz.medida);
        assertEquals(5f, arroz.quantidade);
        assertEquals("7893500020110", arroz.codigoBarras);
    }

    @Test
    void produtoVendidoPorKgSemMarca() {
        ScrapedProdutoDTO patinho = AtacadaoScraper.parseBusca(RESPOSTA).produtos().get("102").paraDto(new BigDecimal("39.99"));

        assertEquals("Carnes, Aves e Peixes", patinho.categoria);
        assertEquals("kg", patinho.medida);
        assertEquals(1f, patinho.quantidade);
        assertNull(patinho.marca);
        assertNull(patinho.codigoBarras); // código de barras é opcional
    }

    @Test
    void embalagemComVariasUnidades() {
        ScrapedProdutoDTO cerveja = AtacadaoScraper.parseBusca(RESPOSTA).produtos().get("104").paraDto(new BigDecimal("37.50"));

        assertEquals("ml", cerveja.medida);
        assertEquals(350f, cerveja.quantidade);
    }

    @Test
    void respostaSemProdutos() {
        AtacadaoScraper.PaginaBusca r = AtacadaoScraper.parseBusca("{\"products\":[],\"recordsFiltered\":0}");
        assertEquals(0, r.quantidadeNaPagina());
        assertEquals(0, r.produtos().size());
    }

    @Test
    void filialDoCep() {
        // resposta real para o CEP de Cuiabá
        AtacadaoScraper.Filial filial = AtacadaoScraper.parseFilial("""
                [{"id":"v2.B44D2E1EE71D0CBE6A770F476DC85B48","sellers":[{"id":"atacadaobr927","name":"Tijucal","logo":""}]}]
                """);
        assertEquals("v2.B44D2E1EE71D0CBE6A770F476DC85B48", filial.regionId());
        assertEquals("atacadaobr927", filial.seller());
    }

    @Test
    void cepSemFilial() {
        // resposta real para CEPs de Porto Velho
        assertNull(AtacadaoScraper.parseFilial("[{\"id\":\"v2.1BB18CE648B5111D0933734ED83EC783\",\"sellers\":[]}]"));
    }
}
