package org.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProdutoPrecoSyncServiceTest {

    @Test
    void codigoDeBarrasNormalizado() {
        assertEquals("7891000462300", ProdutoPrecoSyncService.normalizarCodigoBarras("7891000462300"));
        assertEquals("7891000462300", ProdutoPrecoSyncService.normalizarCodigoBarras("07891000462300")); // GTIN-14
        assertEquals("7891000462300", ProdutoPrecoSyncService.normalizarCodigoBarras(" 789 1000 46230-0 "));
        assertEquals("78924345", ProdutoPrecoSyncService.normalizarCodigoBarras("78924345"));            // EAN-8
    }

    @Test
    void codigoDeBarrasAusenteOuInvalido() {
        assertNull(ProdutoPrecoSyncService.normalizarCodigoBarras(null));
        assertNull(ProdutoPrecoSyncService.normalizarCodigoBarras(""));
        assertNull(ProdutoPrecoSyncService.normalizarCodigoBarras("0000000"));
        assertNull(ProdutoPrecoSyncService.normalizarCodigoBarras("12345")); // código interno, não é EAN
    }
}
