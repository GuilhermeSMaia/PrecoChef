package org.scraper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.scraper.dto.ScrapedProdutoDTO;

class Meta21ScraperTest {

    private static final Instant AGORA = Instant.parse("2026-10-16T12:00:00Z");

    // formato da resposta de /api_ecommerce/v5/items
    private static final String PAGINA = """
            {"data":[
              {"id":"a1","item_type":"product","name":"Batata Lays Taco Mexicano 62g","brand":"Lay's","unit_type":"UNI",
               "price_config":{"price":4.99},"stock":{"has_available_stock":true}},
              {"id":"a2","name":"Kit Shampoo + Condicionador Seda 300ml","brand":"Seda","unit_type":"UNI",
               "price_config":{"price":17.99,"price_discount":{"promo_price":14.98,"end_date":"2026-10-20T02:59:59Z"}},
               "stock":{"has_available_stock":true}},
              {"id":"a3","name":"Café Pilão 500g","brand":"Pilão","unit_type":"UNI",
               "price_config":{"price":19.9,"price_discount":{"promo_price":15.9,"end_date":"2026-10-01T02:59:59Z"}},
               "stock":{"has_available_stock":true}},
              {"id":"a4","name":"Queijo  Minas Padrão Piracanjuba","brand":"","unit_type":"KG",
               "price_config":{"price":76.9},"stock":{"has_available_stock":true}},
              {"id":"a5","name":"Ração Pedigree Filhotes Pacote 18kg","brand":"Pedigree","unit_type":"KG",
               "price_config":{"price":199.9},"stock":{"has_available_stock":true}},
              {"id":"a6","name":"Arroz Sem Estoque 5kg","unit_type":"UNI",
               "price_config":{"price":25.9},"stock":{"has_available_stock":false}}
            ],"pagination":{"page":1,"total_pages":170,"total_count":5088}}
            """;

    private List<Meta21Scraper.Item> itens() {
        return Meta21Scraper.parsePagina(PAGINA, AGORA).itens();
    }

    @Test
    void leProdutosDaPagina() {
        Meta21Scraper.Pagina r = Meta21Scraper.parsePagina(PAGINA, AGORA);
        assertEquals(170, r.totalPaginas());
        assertEquals(6, r.itens().size());

        ScrapedProdutoDTO batata = r.itens().get(0).paraDto("Mercearia");
        assertEquals("Batata Lays Taco Mexicano 62g", batata.nome);
        assertEquals("Mercearia", batata.categoria);
        assertEquals("Lay's", batata.marca);
        assertEquals(new BigDecimal("4.99"), batata.preco);
        assertEquals("g", batata.medida);
        assertEquals(62f, batata.quantidade);
    }

    @Test
    void promocaoVigenteEVencida() {
        assertEquals(new BigDecimal("14.98"), itens().get(1).preco()); // promoção até 20/10
        assertEquals(new BigDecimal("19.90"), itens().get(2).preco()); // promoção já acabou: preço normal
    }

    @Test
    void produtoVendidoPorKg() {
        ScrapedProdutoDTO queijo = itens().get(3).paraDto(null);
        assertEquals("Queijo Minas Padrão Piracanjuba", queijo.nome);
        assertEquals("kg", queijo.medida);
        assertEquals(1f, queijo.quantidade);
        assertNull(queijo.marca);

        // marcado como KG, mas com o peso da embalagem no nome
        ScrapedProdutoDTO racao = itens().get(4).paraDto(null);
        assertEquals("kg", racao.medida);
        assertEquals(18f, racao.quantidade);
    }

    @Test
    void semEstoqueDescartado() {
        assertNull(itens().get(5).preco());
        assertNull(itens().get(5).paraDto("Mercearia"));
    }

    @Test
    void categoriaDoDetalhe() {
        Meta21Scraper.Categoria c = Meta21Scraper.parseCategoria("""
                {"data":{"id":"a1","name":"Batata Lays","subcategories":[
                  {"id":"s0","title":"Ofertas","is_main":false,"category":{"id":"c0","title":"Destaques"}},
                  {"id":"s1","title":"Snacks e Aperitivos","is_main":true,
                   "category":{"id":"65216bcc0c9d838df85e8fe7","title":"Mercearia","slug":"mercearia"}}]}}
                """);
        assertEquals("65216bcc0c9d838df85e8fe7", c.id());
        assertEquals("Mercearia", c.titulo());

        assertNull(Meta21Scraper.parseCategoria("{\"data\":{\"id\":\"a1\",\"subcategories\":[]}}"));
    }
}
