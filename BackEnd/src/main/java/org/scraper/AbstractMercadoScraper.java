package org.scraper;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;
import org.scraper.dto.ScrapedProdutoDTO;

public abstract class AbstractMercadoScraper implements MercadoScraper {

    private static final Logger LOG = Logger.getLogger(AbstractMercadoScraper.class);
    private static final long DELAY_ENTRE_PAGINAS_MS = 2000; // evita sobrecarregar/ser bloqueado pelo site

    @Override
    public List<ScrapedProdutoDTO> scrape() throws Exception {
        List<ScrapedProdutoDTO> todosProdutos = new ArrayList<>();

        for (String url : getUrls()) {
            try {
                List<ScrapedProdutoDTO> produtosDaPagina = scrapePagina(url);
                todosProdutos.addAll(produtosDaPagina);
                LOG.infof("[%s] %d produtos coletados em %s", getNomeMercado(), produtosDaPagina.size(), url);
            } catch (Exception e) {
                LOG.errorf(e, "[%s] falha ao coletar página %s - pulando", getNomeMercado(), url);
                // uma página com erro não derruba a coleta das demais
            }

            Thread.sleep(DELAY_ENTRE_PAGINAS_MS);
        }

        return todosProdutos;
    }

    protected abstract List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception;
}