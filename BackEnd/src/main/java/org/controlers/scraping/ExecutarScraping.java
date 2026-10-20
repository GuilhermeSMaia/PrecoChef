package org.controlers.scraping;

import java.util.List;

import org.scraper.MercadoScraper;
import org.scraper.ProdutoPrecoSyncService;
import org.scraper.dto.ScrapedProdutoDTO;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

// dispara o webscraping na hora, sem esperar o cron. Ex.: POST /scraping/executar?mercado=Irmãos Gonçalves
@Path("/scraping")
public class ExecutarScraping {

    @Inject
    Instance<MercadoScraper> scrapers;

    @Inject
    ProdutoPrecoSyncService syncService;

    @POST
    @Path("/executar")
    public Response executar(@QueryParam("mercado") String mercado) {
        for (MercadoScraper scraper : scrapers) {
            if (mercado != null && !scraper.getNomeMercado().equalsIgnoreCase(mercado)) continue;

            try {
                List<ScrapedProdutoDTO> produtos = scraper.scrape();
                syncService.sincronizar(scraper.getNomeMercado(), produtos);
                return Response.ok(produtos.size() + " produtos coletados de " + scraper.getNomeMercado()).build();
            } catch (Exception e) {
                return Response.serverError().entity("Falha no scraping: " + e.getMessage()).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Nenhum scraper para o mercado informado").build();
    }
}
