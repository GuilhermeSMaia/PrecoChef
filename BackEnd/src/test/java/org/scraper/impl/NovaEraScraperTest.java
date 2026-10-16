package org.scraper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.scraper.dto.ScrapedProdutoDTO;

class NovaEraScraperTest {

    // formato da resposta de /api/io/_v/api/intelligent-search/product_search/category-1/{departamento}
    private static final String BUSCA = """
            {"products":[
              {"productId":"1395508","productName":"Açúcar Doce Dia Cristal Pacote 1kg","brand":"Doce Dia",
               "categories":["/Alimentos/Arroz, Feijão, Açúcar e Grãos/","/Alimentos/"],
               "items":[{"itemId":"101","measurementUnit":"un","unitMultiplier":1,
                 "sellers":[{"sellerId":"1","commertialOffer":{"Price":2.79,"AvailableQuantity":99999}}]}]},
              {"productId":"1407738","productName":"Limao Taiti kg","brand":"",
               "categories":["/Alimentos/Hortifruti/","/Alimentos/"],
               "items":[{"itemId":"102","measurementUnit":"kg","unitMultiplier":0.2,"sellers":[]}]},
              {"productId":"1405015","productName":"Cerveja Amstel Lager Lata 12x350ml","brand":"Amstel",
               "categories":["/Bebidas/Cervejas/","/Bebidas/","/Destaques/","/Destaques/Encarte/"],
               "items":[{"itemId":"103","measurementUnit":"un","unitMultiplier":1,"sellers":[]}]},
              {"productId":"999","productName":"Sem SKU","items":[]}
            ],"recordsFiltered":6411}
            """;

    // formato da resposta de /api/checkout/pub/orderForms/simulation (valores em centavos)
    private static final String SIMULACAO = """
            {"items":[
              {"id":"101","price":339,"listPrice":339,"sellingPrice":329,"unitMultiplier":1,"availability":"available"},
              {"id":"102","price":1399,"listPrice":1399,"sellingPrice":279,"unitMultiplier":0.2,"availability":"available"},
              {"id":"103","price":0,"listPrice":0,"sellingPrice":0,"unitMultiplier":1,"availability":"withoutStock"},
              {"id":"104","price":579,"listPrice":579,"sellingPrice":99,"unitMultiplier":0.2,"availability":"available"}
            ]}
            """;

    @Test
    void leProdutosDaBusca() {
        NovaEraScraper.PaginaBusca r = NovaEraScraper.parseBusca(BUSCA);

        assertEquals(6411, r.total());
        assertEquals(3, r.produtos().size()); // produto sem SKU é descartado

        NovaEraScraper.ProdutoBusca acucar = r.produtos().get(0);
        assertEquals("1395508", acucar.productId());
        assertEquals("101", acucar.itemId());
        assertEquals("Alimentos", acucar.categoria()); // departamento vira a categoria
        assertEquals("Doce Dia", acucar.marca());
        assertFalse(acucar.porKg());

        NovaEraScraper.ProdutoBusca limao = r.produtos().get(1);
        assertNull(limao.marca());
        assertTrue(limao.porKg());
    }

    @Test
    void precosDaSimulacao() {
        Map<String, BigDecimal> precos = NovaEraScraper.parseSimulacao(SIMULACAO);

        assertEquals(new BigDecimal("3.29"), precos.get("101")); // preço final, com desconto
        assertEquals(new BigDecimal("13.99"), precos.get("102")); // kg sem desconto: arredondamento da fração é ignorado
        assertNull(precos.get("103"));                            // sem estoque na loja
        assertEquals(new BigDecimal("4.95"), precos.get("104")); // kg com desconto: 0,99 / 0,2 kg
    }

    @Test
    void produtoVendidoPorKg() {
        NovaEraScraper.ProdutoBusca limao = NovaEraScraper.parseBusca(BUSCA).produtos().get(1);
        ScrapedProdutoDTO dto = limao.paraDto(new BigDecimal("13.99"));

        assertEquals("kg", dto.medida);
        assertEquals(1f, dto.quantidade);
        assertEquals("Alimentos", dto.categoria);
    }

    @Test
    void embalagemComVariasUnidades() {
        ScrapedProdutoDTO cerveja = NovaEraScraper.parseBusca(BUSCA).produtos().get(2).paraDto(new BigDecimal("3.79"));

        assertEquals("ml", cerveja.medida);
        assertEquals(350f, cerveja.quantidade);
        assertEquals("Amstel", cerveja.marca);
        assertEquals("Bebidas", cerveja.categoria); // coleções como "Destaques" não viram categoria
    }

    @Test
    void regiaoDoCep() {
        assertEquals("U1cjbWVyY2FudGlsbm92YWVyYWxvamEyOA==", NovaEraScraper.parseRegionId(
                "[{\"id\":\"U1cjbWVyY2FudGlsbm92YWVyYWxvamEyOA==\",\"sellers\":[{\"id\":\"mercantilnovaeraloja28\"}]}]"));
        assertNull(NovaEraScraper.parseRegionId("[{\"id\":\"U1cj\",\"sellers\":[]}]")); // CEP não atendido
    }

    @Test
    void arvoreDeCategorias() {
        NovaEraScraper scraper = new NovaEraScraper();
        scraper.baseUrl = "https://www.supernovaera.com.br";

        List<NovaEraScraper.Categoria> arvore = scraper.lerArvore("""
                [{"id":50000,"name":"Alimentos","url":"https://www.supernovaera.com.br/alimentos","children":[
                  {"id":10926,"name":"Café","url":"https://www.supernovaera.com.br/alimentos/cafe","children":[]}]},
                 {"id":50027,"name":"Bebidas","url":"https://www.supernovaera.com.br/bebidas","children":[]}]
                """);

        assertEquals(2, arvore.size());
        assertEquals(List.of("alimentos"), arvore.get(0).caminho());
        assertEquals(List.of("alimentos", "cafe"), arvore.get(0).filhas().get(0).caminho());
        assertTrue(arvore.get(1).filhas().isEmpty());
    }

    @Test
    void respostaSemProdutos() {
        NovaEraScraper.PaginaBusca r = NovaEraScraper.parseBusca("{\"products\":[],\"recordsFiltered\":0}");
        assertEquals(0, r.total());
        assertTrue(r.produtos().isEmpty());
    }
}
