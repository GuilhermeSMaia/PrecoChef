package org.scraper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.scraper.dto.ScrapedProdutoDTO;

class IrmaosGoncalvesScraperTest {

    // trecho real da resposta de /api/produto/pesquisar?categoria=/&pagina=3&janela=true
    private static final String RESPOSTA = """
            {"categorias":[{"id":4,"nome":"Açougue","url":"/categoria/açougue","quantidadeProdutos":262,"filhos":[]}],
             "marcas":[{"marca":"3 Corações","quantidadeProdutos":128}],
             "precos":[{"inicio":0,"fim":15,"quantidadeProdutos":9322}],
             "produtos":[
              {"categoria":"Cuidados Pessoais","categoriaId":66,"departamento":"Higiene e Perfumaria","departamentoId":9,"estoque":1,
               "id":"1752","imagem":"/p1752/01KC5GVDCXV36BQY8MY9BK45WA.png","limitacaoQuantidade":10,"marca":"Always",
               "nome":"Absorvente Always com Abas Suave Super Proteção 32 Unidades","pesoUnitario":0,"referencia":"RT3ESKM",
               "subCategoria":"Absorventes","subCategoriaId":363,"unidade":"UND",
               "url":"/absorvente-always-com-abas-suave-super-proteção-32-unidades","valor":18.15,"carrinho":0,"favorito":0},
              {"categoria":"Cuidados Pessoais","departamento":"Higiene e Perfumaria","id":"11268","marca":"Intimus",
               "nome":"Absorvente Interno Intimus Médio 8 Unidades","unidade":"UND","valor":12.4},
              {"categoria":"Carne Bovina","departamento":"Açougue","id":"900","marca":"Frigon",
               "nome":"Carne Bovina Patinho","unidade":"KG","valor":42.9},
              {"categoria":"Refrigerantes","departamento":"Bebidas","id":"901","marca":"Coca-Cola",
               "nome":"Refrigerante Coca-Cola 2L","unidade":"UND","valor":11.99,"valorPromocional":9.49},
              {"departamento":"Bebidas","id":"902","nome":"Sem preço","unidade":"UND","valor":0}
             ],
             "conteudo":{"descricao":""},"janela":true,"paginas":1011,"qtdProdutos":16172}
            """;

    @Test
    void leProdutosEPaginasDaApi() {
        IrmaosGoncalvesScraper.Resposta r = IrmaosGoncalvesScraper.parseResposta(RESPOSTA);

        assertEquals(1011, r.paginas());
        assertEquals(4, r.produtos().size()); // o produto sem preço é descartado

        ScrapedProdutoDTO absorvente = r.produtos().get("1752");
        assertEquals("Absorvente Always com Abas Suave Super Proteção 32 Unidades", absorvente.nome);
        assertEquals("Higiene e Perfumaria", absorvente.categoria); // departamento vira a categoria
        assertEquals("Always", absorvente.marca);
        assertEquals(new BigDecimal("18.15"), absorvente.preco);
        assertEquals("un", absorvente.medida);
        assertEquals(32f, absorvente.quantidade);

        assertEquals(new BigDecimal("12.40"), r.produtos().get("11268").preco);
    }

    @Test
    void produtoVendidoPorKg() {
        ScrapedProdutoDTO carne = IrmaosGoncalvesScraper.parseResposta(RESPOSTA).produtos().get("900");

        assertEquals("Açougue", carne.categoria);
        assertEquals("kg", carne.medida);
        assertEquals(1f, carne.quantidade);
        assertEquals(new BigDecimal("42.90"), carne.preco);
    }

    @Test
    void precoPromocionalTemPrioridade() {
        ScrapedProdutoDTO coca = IrmaosGoncalvesScraper.parseResposta(RESPOSTA).produtos().get("901");

        assertEquals(new BigDecimal("9.49"), coca.preco);
        assertEquals("l", coca.medida);
        assertEquals(2f, coca.quantidade);
    }

    @Test
    void respostaSemProdutos() {
        IrmaosGoncalvesScraper.Resposta r = IrmaosGoncalvesScraper.parseResposta("{\"produtos\":[],\"paginas\":1011}");
        assertEquals(List.of(), List.copyOf(r.produtos().values()));
    }
}
