package org.scraper.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.scraper.dto.ScrapedProdutoDTO;

class Meta21AoVivoTmpTest {
    @Test
    void coletaReal() throws Exception {
        Meta21Scraper s = new Meta21Scraper();
        s.nomeMercado = "Meta 21";
        s.apiUrl = "https://api.ibecom.com.br";
        s.storeId = "64ef45de247bbb50dda51202";
        s.delayMs = 1500;
        s.esperaBloqueioMs = 60000;
        long t = System.currentTimeMillis();
        List<ScrapedProdutoDTO> ps = s.scrape();
        System.out.println("RESULTADO total=" + ps.size() + " em " + (System.currentTimeMillis() - t) / 1000 + "s");
        Map<String, Long> porCat = ps.stream().collect(Collectors.groupingBy(p -> String.valueOf(p.categoria), Collectors.counting()));
        System.out.println("RESULTADO categorias=" + porCat);
        ps.stream().filter(p -> p.medida.equals("kg")).limit(4).forEach(p ->
                System.out.println("RESULTADO kg: " + p.nome + " | " + p.preco + " | " + p.quantidade + " " + p.medida));
        ps.stream().limit(4).forEach(p ->
                System.out.println("RESULTADO " + p.nome + " | " + p.preco + " | " + p.quantidade + " " + p.medida + " | " + p.categoria));
    }
}
