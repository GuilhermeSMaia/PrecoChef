package org.scraper.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.scraper.AbstractMercadoScraper;
import org.scraper.dto.ScrapedProdutoDTO;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MercadoModeloScraper extends AbstractMercadoScraper {

    // uma URL por categoria/departamento - adicionar quantas forem necessárias
    private static final List<String> URLS = List.of(
            "https://www.mercado-exemplo.com.br/categoria/hortifruti",
            "https://www.mercado-exemplo.com.br/categoria/acougue",
            "https://www.mercado-exemplo.com.br/categoria/padaria",
            "https://www.mercado-exemplo.com.br/categoria/limpeza"
    );

    @Override
    public String getNomeMercado() {
        return "Mercado Modelo";
    }

    @Override
    public List<String> getUrls() {
        return URLS;
    }

    @Override
    protected List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception {
        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        String categoria = extrairCategoriaDaUrl(url);

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; PrecoChefBot/1.0)")
                .timeout(15_000)
                .get();

        Elements cards = doc.select(".product-card"); // ajustar seletor real do site

        for (Element card : cards) {
            String nome = card.select(".product-card__name").text();
            String precoTexto = card.select(".product-card__price").text();
            String medida = card.select(".product-card__unit").text();

            if (nome.isBlank() || precoTexto.isBlank()) continue;

            BigDecimal preco = parsePreco(precoTexto);
            if (preco == null) continue;

            produtos.add(new ScrapedProdutoDTO(nome.trim(), categoria, medida.trim(), preco));
        }

        return produtos;
    }

    private String extrairCategoriaDaUrl(String url) {
        String slug = url.substring(url.lastIndexOf('/') + 1);
        return switch (slug) {
            case "hortifruti" -> "Hortifruti";
            case "acougue" -> "Açougue";
            case "padaria" -> "Padaria";
            case "limpeza" -> "Limpeza";
            default -> "Outros";
        };
    }

    private BigDecimal parsePreco(String texto) {
        try {
            String limpo = texto.replaceAll("[^0-9,]", "").replace(",", ".");
            return new BigDecimal(limpo);
        } catch (Exception e) {
            return null;
        }
    }
}