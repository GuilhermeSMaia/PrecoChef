package org.scraper.juncao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

// nomes reais do Meta 21 (item) x Atacadão / Nova Era (candidatos)
class CasadorPorNomeTest {

    private static String casar(String item, String marcaItem, String... candidatos) {
        CasadorPorNome<String> casador = new CasadorPorNome<>();
        for (String c : candidatos) casador.adicionar(c, c, null);
        return casador.encontrar(item, marcaItem);
    }

    @Test
    void mesmasPalavrasEmOutraOrdem() {
        assertEquals("Cerveja Skol Pilsen 269ml",
                casar("Cerveja Pilsen Skol Lata 269ml", "Skol", "Cerveja Skol Pilsen 269ml"));
        assertEquals("Achocolatado em Pó 3 Corações Chocolatto 900g",
                casar("Achocolatado Po 3 Coracoes 900G Chocolatto", "3 Corações", "Achocolatado em Pó 3 Corações Chocolatto 900g"));
    }

    @Test
    void tamanhoEscritoDeFormasDiferentes() {
        assertEquals("Iogurte Nestlé Zero Batido 1,15kg",
                casar("Iogurte Nestle 1.150Kg Batido Zero", "Nestlé", "Iogurte Nestlé Zero Batido 1,15kg"));
        assertEquals("Lava Roupas Líquido Coala Laundry Orquídea Negra 5L",
                casar("Lava-roupas Líquido Coala Laundry Orquídea Negra 5000ml", "Coala", "Lava Roupas Líquido Coala Laundry Orquídea Negra 5L"));
    }

    @Test
    void nomeMaisCurtoContidoNoMaisLongo() {
        assertEquals("Lombo de Bacalhau Costa Sul Congelado Dessalgado 600g",
                casar("Bacalhau Congelado Costa Sul 600g Lombo", "Costa Sul", "Lombo de Bacalhau Costa Sul Congelado Dessalgado 600g"));
    }

    @Test
    void abreviacaoDeRecheado() {
        assertEquals("Chocolate Recheado Garoto Baton 90g",
                casar("Chocolate 90G Barra Garoto Baton Rech", "Garoto", "Chocolate Recheado Garoto Baton 90g"));
    }

    @Test
    void saborDiferenteNaoLiga() {
        assertNull(casar("Difusor Aromas Coala 100Ml Patchouli Tangerina", "Coala", "Difusor de Aromas Coala Lavanda 100ml"));
        assertNull(casar("Iogurte Desnatado Ameixa Zero Lactose Batavo Pense Zero 1,15kg", "Batavo",
                "Iogurte Batavo Pense Zero Mamão Zero Lactose Garrafa 1,15kg"));
        assertNull(casar("Biscoito Distração Chocolate com Baunilha Marilan 280g", "Marilan",
                "Biscoito Amanteigado Marilan Chocolate 280g"));
    }

    @Test
    void palavraDeVersaoEmSoUmDosNomesNaoLiga() {
        assertNull(casar("Molho Shoyu Light Mitsuwa 500Ml", "Mitsuwa", "Molho Shoyu Mitsuwa 500ml"));
        assertNull(casar("Sardinha com Óleo Limão Gomes da Costa Lata 75g", "Gomes da Costa", "Sardinha Gomes da Costa Óleo 75g"));
        assertNull(casar("Sorvete Napolitano Nestlé Pote 450g", "Nestlé", "Sorvete Nestlé Napolitano Zero 450g"));
        assertNull(casar("Chocolate ao Leite Lacta Pacote 145g", "Lacta", "Chocolate Lacta Shot Ao Leite com Amendoim 145g"));
        assertNull(casar("Margarina Qualy Sem Sal 500g", "Qualy", "Margarina Qualy 500g"));
        assertNull(casar("Oral B 3D Creme dental Mineral Clean 70G", "Oral-B", "Creme Dental Oral-B 1-2-3 70g"));
    }

    @Test
    void comboNaoLiga() {
        assertNull(casar("Cereal Matinal NESCAU 210g + Snow Flakes 230g", "Nestlé", "Cereal Matinal SNOW FLAKES Nestlé 230g"));
    }

    @Test
    void tamanhoDiferenteNaoLiga() {
        assertNull(casar("Cerveja Skol Pilsen Lata 350ml", "Skol", "Cerveja Skol Pilsen 269ml"));
    }

    @Test
    void packNaoLigaComAvulso() {
        assertNull(casar("Cerveja Skol Pilsen c/15uni 269ML", "Skol", "Cerveja Skol Pilsen 269ml"));
        assertNull(casar("Chiclete Trident Hortelã sem açúcar 21x8g", "Trident", "Chiclete Trident Hortelã sem açúcar 8g"));
        assertNull(casar("Kit Shampoo Seda Ceramidas 325ml", "Seda", "Shampoo Seda Ceramidas 325ml"));
    }

    @Test
    void marcaDiferenteNaoLiga() {
        assertNull(casar("Leite Condensado Moça 395g", "Nestlé", "Leite Condensado Piracanjuba 395g"));
    }

    @Test
    void semMarcaEmNenhumLadoNaoLiga() {
        assertNull(casar("Leite Condensado 395g", null, "Leite Condensado Integral 395g"));
    }

    @Test
    void empateNaoLiga() {
        // dois candidatos igualmente parecidos: não dá para saber qual é
        assertNull(casar("Sabonete Albany 80G Gelato", "Albany", "Sabonete Albany Gelato 80g", "Sabonete Albany 80g Gelato"));
    }

    @Test
    void candidatoRemovidoNaoEhMaisEncontrado() {
        CasadorPorNome<String> casador = new CasadorPorNome<>();
        casador.adicionar("skol", "Cerveja Skol Pilsen 269ml", "Skol");
        casador.remover("skol");
        assertNull(casador.encontrar("Cerveja Pilsen Skol Lata 269ml", "Skol"));
    }

    @Test
    void tamanhoNormalizado() {
        assertEquals("ml:900.0", CasadorPorNome.tamanho("oleo soya 900ml"));
        assertEquals("g:1150.0", CasadorPorNome.tamanho("iogurte 1,15kg"));
        assertEquals("ml:1500.0", CasadorPorNome.tamanho("refrigerante 1.5l"));
        assertNull(CasadorPorNome.tamanho("queijo minas padrao"));
    }
}
