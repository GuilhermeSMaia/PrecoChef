package org.scraper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.scraper.dto.ScrapedProdutoDTO;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

class Meta21ScraperTest {

    private static final Instant AGORA = Instant.parse("2026-10-16T12:00:00Z");

    // dados do produto como vêm em "initialData":{"data":...} na página /p/{slug}
    private static final String ARROZ = """
            {"id":"6a5166061de88150883d7d7e","item_type":"product","name":"Arroz Bernardo 1Kg Tipo 1",
             "slug":"arroz-bernardo-1kg-tipo-1","brand":"Bernardo","unit_type":"UNI",
             "price_config":{"price":4.59},"stock":{"has_available_stock":true,"max_purchase_quantity":1476},
             "description":"Arroz {tipo 1} \\"agulhinha\\"",
             "subcategories":[
               {"id":"s0","title":"Ofertas","is_main":false,"category":{"id":"c0","title":"Destaques"}},
               {"id":"6a708c0f1b8d572acdb1807d","title":"Arroz","is_main":true,
                "category":{"id":"65216bcb0c9d838df85e8fb6","title":"Alimentos Básicos","slug":"alimentos-basicos"}}]}
            """;

    // o Next.js manda o payload em pedaços de texto; aqui o JSON do produto fica partido entre dois
    private static String paginaProduto(String dados) {
        String payload = "2:I[1,[],\"x\"]\n32:[\"$\",\"$L46\",null,{\"slug\":\"x\",\"initialData\":{\"data\":"
                + dados.strip() + ",\"headers\":{\"cache-control\":\"public\"}}}]\n";
        int meio = payload.length() / 2;
        return "<html><body><script>self.__next_f.push([0])</script>"
                + push(payload.substring(0, meio)) + "<div>Arroz</div>" + push(payload.substring(meio))
                + "</body></html>";
    }

    private static String push(String texto) {
        return "<script>self.__next_f.push(" + Json.createArrayBuilder().add(1).add(texto).build() + ")</script>";
    }

    private static Meta21Scraper.Item item(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return Meta21Scraper.item(reader.readObject(), AGORA);
        }
    }

    @Test
    void leProdutoDaPagina() {
        Meta21Scraper.Produto p = Meta21Scraper.parseProduto(paginaProduto(ARROZ), AGORA);
        assertEquals("Alimentos Básicos", p.categoria());

        ScrapedProdutoDTO arroz = p.item().paraDto(p.categoria());
        assertEquals("Arroz Bernardo 1Kg Tipo 1", arroz.nome);
        assertEquals("Alimentos Básicos", arroz.categoria);
        assertEquals("Bernardo", arroz.marca);
        assertEquals(new BigDecimal("4.59"), arroz.preco);
        assertEquals("kg", arroz.medida);
        assertEquals(1f, arroz.quantidade);
    }

    @Test
    void paginaSemDadosDoProduto() {
        assertNull(Meta21Scraper.parseProduto("<html><title>Produto não encontrado</title></html>", AGORA));
        assertNull(Meta21Scraper.parseProduto(push("1:[\"$\",\"div\",null,{}]"), AGORA));
    }

    @Test
    void promocaoVigenteEVencida() {
        assertEquals(new BigDecimal("14.98"), item("""
                {"id":"a2","name":"Kit Shampoo + Condicionador Seda 300ml","unit_type":"UNI",
                 "price_config":{"price":17.99,"price_discount":{"promo_price":14.98,"end_date":"2026-10-20T02:59:59Z"}}}
                """).preco());
        assertEquals(new BigDecimal("19.90"), item("""
                {"id":"a3","name":"Café Pilão 500g","unit_type":"UNI",
                 "price_config":{"price":19.9,"price_discount":{"promo_price":15.9,"end_date":"2026-10-01T02:59:59Z"}}}
                """).preco());
    }

    @Test
    void produtoVendidoPorKg() {
        ScrapedProdutoDTO queijo = item("""
                {"id":"a4","name":"Queijo  Minas Padrão Piracanjuba","brand":"","unit_type":"KG","price_config":{"price":76.9}}
                """).paraDto(null);
        assertEquals("Queijo Minas Padrão Piracanjuba", queijo.nome);
        assertEquals("kg", queijo.medida);
        assertEquals(1f, queijo.quantidade);
        assertNull(queijo.marca);

        // marcado como KG, mas com o peso da embalagem no nome
        ScrapedProdutoDTO racao = item("""
                {"id":"a5","name":"Ração Pedigree Filhotes Pacote 18kg","unit_type":"KG","price_config":{"price":199.9}}
                """).paraDto(null);
        assertEquals("kg", racao.medida);
        assertEquals(18f, racao.quantidade);
    }

    @Test
    void semEstoqueDescartado() {
        Meta21Scraper.Item semEstoque = item("""
                {"id":"a6","name":"Arroz Sem Estoque 5kg","unit_type":"UNI",
                 "price_config":{"price":25.9},"stock":{"has_available_stock":false}}
                """);
        assertNull(semEstoque.preco());
        assertNull(semEstoque.paraDto("Mercearia"));
    }

    @Test
    void categoriaSemPrincipal() {
        JsonObject p = Json.createReader(new StringReader("""
                {"subcategories":[{"is_main":false,"category":{"title":"Bebidas"}},{"category":{"title":"Outra"}}]}
                """)).readObject();
        assertEquals("Bebidas", Meta21Scraper.categoria(p));
        assertNull(Meta21Scraper.categoria(Json.createObjectBuilder().build()));
    }

    @Test
    void urlsDoSitemap() {
        List<String> urls = Meta21Scraper.parseSitemap("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <url><loc>https://supermercadometa21.instabuy.app.br</loc></url>
                <url><loc>https://supermercadometa21.instabuy.app.br/sub/vinhos-e-espumantes/espumante</loc></url>
                <url><loc>https://supermercadometa21.instabuy.app.br/p/arroz-bernardo-1kg-tipo-1</loc></url>
                <url><loc>https://supermercadometa21.instabuy.app.br/p/batata-lays-taco-mexicano-62g</loc></url>
                <url><loc>https://supermercadometa21.instabuy.app.br/p/arroz-bernardo-1kg-tipo-1</loc></url>
                </urlset>
                """);
        assertEquals(List.of(
                "https://supermercadometa21.instabuy.app.br/p/arroz-bernardo-1kg-tipo-1",
                "https://supermercadometa21.instabuy.app.br/p/batata-lays-taco-mexicano-62g"), urls);
    }
}
