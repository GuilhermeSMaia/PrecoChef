package org.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MedidaParserTest {

    private void assertMedida(String nome, float qtd, String unidade) {
        MedidaParser.Medida m = MedidaParser.parse(nome);
        assertEquals(qtd, m.quantidade(), 0.001, nome);
        assertEquals(unidade, m.unidade(), nome);
    }

    @Test
    void extraiQuantidadeEUnidade() {
        assertMedida("Arroz Tio João Tipo 1 5kg", 5, "kg");
        assertMedida("Refrigerante Coca-Cola 2L", 2, "l");
        assertMedida("Leite Integral Italac 1 Litro", 1, "l");
        assertMedida("Cerveja Skol Lata 12x350ml", 350, "ml");
        assertMedida("Café Melitta 500g", 500, "g");
        assertMedida("Biscoito Recheado 130 Gr", 130, "g");
        assertMedida("Óleo de Soja Soya 900 ml", 900, "ml");
        assertMedida("Feijão Carioca 1,5 Kg", 1.5f, "kg");
        assertMedida("Ovos Brancos 12 Unidades", 12, "un");
    }

    @Test
    void granelEPadrao() {
        assertMedida("Banana Prata Kg", 1, "kg");
        assertMedida("Alface Crespa", 1, "un");
    }
}
