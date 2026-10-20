package org.scraper;

import java.util.List;

import org.jboss.logging.Logger;
import org.scraper.dto.ScrapedProdutoDTO;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ScrapingScheduler {

    private static final Logger LOG = Logger.getLogger(ScrapingScheduler.class);

    @Inject
    Instance<MercadoScraper> scrapers; // injeta automaticamente cada @ApplicationScoped que implementa MercadoScraper

    @Inject
    ProdutoPrecoSyncService syncService;

    @Scheduled(cron = "{scraping.cron.expr}")
    void executarScraping() {
        LOG.info("Iniciando ciclo de atualização de preços via webscraping...");

        for (MercadoScraper scraper : scrapers) {
            try {
                List<ScrapedProdutoDTO> produtos = scraper.scrape();
                syncService.sincronizar(scraper.getNomeMercado(), produtos);
            } catch (Exception e) {
                LOG.errorf(e, "Falha ao coletar dados do mercado '%s'", scraper.getNomeMercado());
                // segue para o próximo mercado mesmo se um falhar
            }
        }

        LOG.info("Ciclo de atualização de preços finalizado.");
    }
}