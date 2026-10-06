package org.scraper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.scraper.dto.ScrapedProdutoDTO;

class IrmaosGoncalvesScraperTest {

    private static final String BASE = "https://www.irmaosgoncalves.com.br";

    @Test
    void descobreDepartamentosDoMenu() {
        Document home = Jsoup.parse("""
                <nav>
                  <a href="/categoria/mercearia">Mercearia</a>
                  <a href="/categoria/mercearia/cafe">Café</a>
                  <a href="/categoria/bebidas/refrigerantes">Refrigerantes</a>
                  <a href="/categoria/congelados--resfriados-e-sobremesas">Congelados, Resfriados e Sobremesas</a>
                  <a href="/quem-somos">Quem somos</a>
                </nav>""", BASE);

        Map<String, String> deps = IrmaosGoncalvesScraper.descobrirDepartamentos(home, BASE);

        assertEquals(3, deps.size());
        assertEquals("Mercearia", deps.get(BASE + "/categoria/mercearia"));
        assertEquals("Bebidas", deps.get(BASE + "/categoria/bebidas")); // só havia link da subcategoria
        assertEquals("Congelados, Resfriados e Sobremesas", deps.get(BASE + "/categoria/congelados--resfriados-e-sobremesas"));
    }

    @Test
    void nomeDoSlug() {
        assertEquals("Higiene e Cuidados Pessoais", IrmaosGoncalvesScraper.nomeDoSlug("higiene-e-cuidados-pessoais"));
        assertEquals("Doces, Balas e Chocolates", IrmaosGoncalvesScraper.nomeDoSlug("doces--balas-e-chocolates"));
        assertEquals("bebidas", IrmaosGoncalvesScraper.slugDepartamento(BASE + "/categoria/bebidas/refrigerantes?page=2"));
    }

    @Test
    void extraiDeJsonLd() {
        Document doc = Jsoup.parse("""
                <script type="application/ld+json">
                {"@context":"https://schema.org","@type":"ItemList","itemListElement":[
                  {"@type":"ListItem","position":1,"item":{"@type":"Product","name":"Arroz Tio João 5kg","offers":{"@type":"Offer","price":"27.90"}}},
                  {"@type":"ListItem","position":2,"item":{"@type":"Product","name":"Feijão Carioca 1kg","offers":{"price":8.49}}}
                ]}
                </script>""");

        List<ScrapedProdutoDTO> ps = IrmaosGoncalvesScraper.extrairProdutos(doc, "Mercearia", null, null);

        assertEquals(2, ps.size());
        assertEquals("Arroz Tio João 5kg", ps.get(0).nome);
        assertEquals(new BigDecimal("27.90"), ps.get(0).preco);
        assertEquals("kg", ps.get(0).medida);
        assertEquals(5f, ps.get(0).quantidade);
        assertEquals("Mercearia", ps.get(1).categoria);
    }

    @Test
    void extraiDeNextData() {
        Document doc = Jsoup.parse("""
                <script id="__NEXT_DATA__" type="application/json">
                {"props":{"pageProps":{"categoria":{"nome":"Bebidas"},"produtos":[
                  {"id":1,"descricao":"Refrigerante Coca-Cola 2L","preco":"10,99","precoPromocional":"9,49"},
                  {"id":2,"descricao":"Água Mineral 500ml","preco":2.5}
                ]}}}
                </script>""");

        List<ScrapedProdutoDTO> ps = IrmaosGoncalvesScraper.extrairProdutos(doc, "Bebidas", null, null);

        assertEquals(2, ps.size());
        assertEquals(new BigDecimal("9.49"), ps.get(0).preco); // promocional tem prioridade
        assertEquals("l", ps.get(0).medida);
        assertEquals(new BigDecimal("2.50"), ps.get(1).preco);
        assertEquals("ml", ps.get(1).medida);
    }

    @Test
    void extraiDoHtmlDosCards() {
        Document doc = Jsoup.parse("""
                <div class="product-card">
                  <img alt="Detergente Ypê 500ml">
                  <h3 class="product-card__name">Detergente Ypê 500ml</h3>
                  <span class="old">De R$ 3,49</span> <span class="price">Por R$ 2,79</span>
                </div>
                <div class="product-card"><h3>Sabão em Pó Omo 1,6kg</h3><span>R$ 1.029,90</span></div>
                <div class="product-card"><h3>Sem preço</h3></div>""");

        List<ScrapedProdutoDTO> ps = IrmaosGoncalvesScraper.extrairProdutos(doc, "Limpeza", null, null);

        assertEquals(2, ps.size());
        assertEquals("Detergente Ypê 500ml", ps.get(0).nome);
        assertEquals(new BigDecimal("2.79"), ps.get(0).preco);
        assertEquals(new BigDecimal("1029.90"), ps.get(1).preco);
        assertTrue(ps.stream().allMatch(p -> "Limpeza".equals(p.categoria)));
    }
}
