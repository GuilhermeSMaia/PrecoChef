package org.scraper;

import java.util.List;

import org.scraper.dto.ScrapedProdutoDTO;

public interface MercadoScraper {

    // precisa bater EXATAMENTE com o "nome" já cadastrado na tabela mercados
    String getNomeMercado();

    // uma URL por categoria/departamento do site do mercado
    List<String> getUrls();

    List<ScrapedProdutoDTO> scrape() throws Exception;
}