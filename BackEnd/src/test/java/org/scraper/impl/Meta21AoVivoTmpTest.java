package org.scraper.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.scraper.dto.ScrapedProdutoDTO;

// coleta real no site (~2h para os 5 mil produtos): só roda manualmente. Amostra de 200 produtos:
// mvnw test -Dtest=Meta21AoVivoTmpTest "-DargLine=-Damostra=200 -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition"
// (sem -Damostra faz a coleta completa)
@Disabled("coleta real e demorada - rodar manualmente")
class Meta21AoVivoTmpTest {
    @Test
    void coletaReal() throws Exception {
        Meta21Scraper s = new Meta21Scraper();
        s.nomeMercado = "Meta 21";
        s.siteUrl = "https://supermercadometa21.instabuy.app.br";
        s.delayMs = 1000;
        s.esperaBloqueioMs = 120000;
        int amostra = Integer.getInteger("amostra", 0); // 0 = coleta completa
        long t = System.currentTimeMillis();

        List<ScrapedProdutoDTO> ps;
        if (amostra > 0) {
            List<String> urls = s.getUrls();
            String sitemap = s.baixar(urls.get(0));
            List<String> produtos = Meta21Scraper.parseSitemap(sitemap);
            System.out.println("RESULTADO sitemap=" + produtos.size());
            ps = new ArrayList<>();
            int falhas = 0;
            for (String url : produtos.subList(0, Math.min(amostra, produtos.size()))) {
                Thread.sleep(s.delayMs);
                try {
                    ps.addAll(s.scrapePagina(url));
                } catch (Exception e) {
                    falhas++;
                    System.out.println("RESULTADO falha " + url + ": " + e.getMessage());
                }
            }
            System.out.println("RESULTADO amostra=" + amostra + " falhas=" + falhas);
        } else {
            ps = s.scrape();
        }

        System.out.println("RESULTADO total=" + ps.size() + " em " + (System.currentTimeMillis() - t) / 1000 + "s");
        Map<String, Long> porCat = ps.stream().collect(Collectors.groupingBy(p -> String.valueOf(p.categoria), Collectors.counting()));
        System.out.println("RESULTADO categorias=" + porCat);
        ps.stream().filter(p -> p.medida.equals("kg")).limit(4).forEach(p ->
                System.out.println("RESULTADO kg: " + p.nome + " | " + p.preco + " | " + p.quantidade + " " + p.medida));
        ps.stream().limit(6).forEach(p ->
                System.out.println("RESULTADO " + p.nome + " | " + p.marca + " | " + p.preco + " | " + p.quantidade + " " + p.medida + " | " + p.categoria));
    }
}
