package org.scraper.impl;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.scraper.AbstractMercadoScraper;
import org.scraper.MedidaParser;
import org.scraper.dto.ScrapedProdutoDTO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Scraper do Supermercado Meta 21 (https://supermercadometa21.instabuy.app.br).
 *
 * A loja roda na plataforma Instabuy/ibecom: o HTML é montado no navegador (Next.js), então os
 * produtos vêm da API pública da plataforma, identificando a loja pelo cabeçalho X-IB-Store-Id:
 *   GET https://api.ibecom.com.br/api_ecommerce/v5/items?page=N&limit=30[&category_id=...]
 *   GET https://api.ibecom.com.br/api_ecommerce/v5/products/{id}   (detalhe, traz a categoria)
 *
 * A API não lista as categorias da loja e a listagem não informa a categoria de cada produto.
 * Por isso a categoria é descoberta assim: lê o detalhe de um produto ainda sem categoria e lista
 * items?category_id=... para rotular de uma vez todos os produtos daquela categoria.
 * A API não informa código de barras.
 */
@ApplicationScoped
public class Meta21Scraper extends AbstractMercadoScraper {

    private static final Logger LOG = Logger.getLogger(Meta21Scraper.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";
    private static final String CAMINHO_API = "/api_ecommerce/v5";
    private static final int ITENS_POR_PAGINA = 30; // máximo aceito pela API
    private static final int MAX_PAGINAS = 1000;    // limite de segurança; o total real vem de "pagination"
    private static final int MAX_TENTATIVAS = 3;

    @ConfigProperty(name = "scraping.meta21.nome-mercado", defaultValue = "Meta 21")
    String nomeMercado;

    @ConfigProperty(name = "scraping.meta21.api-url", defaultValue = "https://api.ibecom.com.br")
    String apiUrl;

    // id da loja na Instabuy (aparece no HTML do site como "store":{"id":...})
    @ConfigProperty(name = "scraping.meta21.store-id", defaultValue = "64ef45de247bbb50dda51202")
    String storeId;

    // a API bloqueia (HTTP 429) quem faz requisições rápido demais
    @ConfigProperty(name = "scraping.meta21.delay-ms", defaultValue = "1500")
    long delayMs;

    // espera após um HTTP 429 (multiplicada pela tentativa: 1x, 2x, 3x)
    @ConfigProperty(name = "scraping.meta21.espera-bloqueio-ms", defaultValue = "60000")
    long esperaBloqueioMs;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String getNomeMercado() {
        return nomeMercado;
    }

    @Override
    public List<String> getUrls() {
        return List.of(urlItens(null, 1));
    }

    @Override
    public List<ScrapedProdutoDTO> scrape() throws Exception {
        Map<String, Item> itens = listar(null);
        if (itens.isEmpty()) {
            throw new IllegalStateException("nenhum produto coletado (verifique scraping.meta21.store-id)");
        }

        Map<String, String> categoriaPorItem = new HashMap<>();
        int categorias = 0;
        for (String id : itens.keySet()) {
            if (categoriaPorItem.containsKey(id)) continue;
            try {
                Categoria categoria = parseCategoria(baixar(apiUrl + CAMINHO_API + "/products/" + id));
                Thread.sleep(delayMs);
                if (categoria == null) {
                    categoriaPorItem.put(id, null); // produto sem categoria: vira "Outros" na sincronização
                    continue;
                }
                listar(categoria.id()).keySet().forEach(i -> categoriaPorItem.putIfAbsent(i, categoria.titulo()));
                categoriaPorItem.putIfAbsent(id, categoria.titulo());
                categorias++;
            } catch (BloqueadoException e) {
                // sem categoria é melhor do que perder a coleta: salva o que já foi coletado
                LOG.errorf("[%s] %s - os produtos restantes ficam sem categoria", nomeMercado, e.getMessage());
                break;
            } catch (Exception e) {
                categoriaPorItem.put(id, null); // não tenta de novo o mesmo produto
                LOG.errorf("[%s] falha ao descobrir a categoria do produto %s: %s", nomeMercado, id, e.getMessage());
            }
        }

        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        for (Item item : itens.values()) {
            ScrapedProdutoDTO dto = item.paraDto(categoriaPorItem.get(item.id()));
            if (dto != null) produtos.add(dto);
        }
        LOG.infof("[%s] coleta concluída: %d produtos com preço (de %d listados) em %d categorias",
                nomeMercado, produtos.size(), itens.size(), categorias);
        return produtos;
    }

    @Override
    protected List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception {
        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        for (Item item : parsePagina(baixar(url)).itens()) {
            ScrapedProdutoDTO dto = item.paraDto(null);
            if (dto != null) produtos.add(dto);
        }
        return produtos;
    }

    // todas as páginas da listagem (da loja inteira ou de uma categoria); chave = id do produto
    private Map<String, Item> listar(String categoriaId) throws Exception {
        Map<String, Item> itens = new LinkedHashMap<>();
        int totalPaginas = 1;
        for (int pagina = 1; pagina <= Math.min(totalPaginas, MAX_PAGINAS); pagina++) {
            try {
                Pagina resposta = parsePagina(baixar(urlItens(categoriaId, pagina)));
                totalPaginas = resposta.totalPaginas();
                resposta.itens().forEach(i -> itens.putIfAbsent(i.id(), i));
            } catch (BloqueadoException e) {
                throw e;
            } catch (Exception e) {
                // uma página com erro não derruba a coleta das demais
                LOG.errorf("[%s] falha na página %d (categoria %s): %s", nomeMercado, pagina, categoriaId, e.getMessage());
                if (pagina == 1) throw e; // sem a primeira página não se sabe o total
            }
            Thread.sleep(delayMs);
        }
        return itens;
    }

    private String urlItens(String categoriaId, int pagina) {
        String url = apiUrl + CAMINHO_API + "/items?page=" + pagina + "&limit=" + ITENS_POR_PAGINA;
        if (categoriaId != null) url += "&category_id=" + URLEncoder.encode(categoriaId, StandardCharsets.UTF_8);
        return url;
    }

    private String baixar(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .header("X-IB-Store-Id", storeId)
                .GET()
                .build();

        Exception ultimoErro = null;
        long espera = 0;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            if (espera > 0) Thread.sleep(espera);
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return response.body();
                if (response.statusCode() == 429) {
                    // a API bloqueia o IP por um tempo quando recebe requisições demais: espera bem mais antes de tentar de novo
                    ultimoErro = new BloqueadoException("API bloqueou por excesso de requisições (HTTP 429) em " + url);
                    espera = esperaBloqueioMs * tentativa;
                    LOG.warnf("[%s] HTTP 429 - aguardando %ds antes de tentar de novo", nomeMercado, espera / 1000);
                    continue;
                }
                ultimoErro = new IllegalStateException("HTTP " + response.statusCode() + " em " + url);
                if (response.statusCode() < 500) break; // erro do pedido, não adianta repetir
            } catch (IOException e) {
                ultimoErro = e;
            }
            espera = 2000L * tentativa;
        }
        throw ultimoErro;
    }

    // a API continua bloqueando mesmo após esperar: insistir só prolonga o bloqueio
    static class BloqueadoException extends IllegalStateException {
        BloqueadoException(String mensagem) {
            super(mensagem);
        }
    }

    // ---------------------------------------------------------------- leitura do JSON

    record Categoria(String id, String titulo) {}

    record Pagina(List<Item> itens, int totalPaginas) {}

    /** preco = null quando o produto está sem estoque ou sem preço. */
    record Item(String id, String nome, String marca, boolean porKg, BigDecimal preco) {
        ScrapedProdutoDTO paraDto(String categoria) {
            if (preco == null) return null;
            MedidaParser.Medida medida = MedidaParser.parse(nome);
            // vendido por kg e sem peso no nome ("Queijo Minas Padrão"): o preço é o do quilo.
            // Com peso no nome ("Ração Pedigree Pacote 18kg") o preço é o da embalagem
            if (porKg && medida.unidade().equals(MedidaParser.PADRAO)) medida = new MedidaParser.Medida(1, "kg");
            ScrapedProdutoDTO dto = new ScrapedProdutoDTO(nome, categoria, medida.unidade(), medida.quantidade(), preco);
            dto.marca = marca;
            return dto;
        }
    }

    /**
     * Formato (resumido):
     * {"data":[{"id":"6a6d...","name":"Batata Lays Taco Mexicano 62g","brand":"Lay's","unit_type":"UNI",
     *   "price_config":{"price":17.99,"price_discount":{"promo_price":14.98,"end_date":"2026-10-20T02:59:59Z"}},
     *   "stock":{"has_available_stock":true}}],
     *  "pagination":{"page":1,"total_pages":170,"total_count":5088}}
     */
    static Pagina parsePagina(String json) {
        return parsePagina(json, Instant.now());
    }

    static Pagina parsePagina(String json, Instant agora) {
        JsonObject body;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            body = reader.readObject();
        }

        List<Item> itens = new ArrayList<>();
        JsonArray lista = body.get("data") instanceof JsonArray a ? a : JsonValue.EMPTY_JSON_ARRAY;
        for (JsonValue valor : lista) {
            if (!(valor instanceof JsonObject p)) continue;
            String id = p.getString("id", "");
            String nome = p.getString("name", "").replaceAll("\\s+", " ").trim();
            if (id.isEmpty() || nome.isEmpty()) continue;

            String marca = p.getString("brand", "").trim();
            boolean porKg = p.getString("unit_type", "").equalsIgnoreCase("KG");
            itens.add(new Item(id, nome, marca.isEmpty() ? null : marca, porKg, preco(p, agora)));
        }

        int totalPaginas = 1;
        if (body.get("pagination") instanceof JsonObject pag && pag.get("total_pages") instanceof JsonNumber n) {
            totalPaginas = n.intValue();
        }
        return new Pagina(itens, totalPaginas);
    }

    // preço promocional vale enquanto não passou do end_date (mesma regra do site)
    private static BigDecimal preco(JsonObject produto, Instant agora) {
        if (produto.get("stock") instanceof JsonObject estoque && !estoque.getBoolean("has_available_stock", true)) return null;
        if (!(produto.get("price_config") instanceof JsonObject config)) return null;

        BigDecimal preco = config.get("price") instanceof JsonNumber n ? n.bigDecimalValue() : null;
        if (config.get("price_discount") instanceof JsonObject desconto
                && desconto.get("promo_price") instanceof JsonNumber promo
                && promo.doubleValue() > 0
                && (preco == null || promo.bigDecimalValue().compareTo(preco) < 0)
                && vigente(desconto.getString("end_date", null), agora)) {
            preco = promo.bigDecimalValue();
        }
        if (preco == null || preco.signum() <= 0) return null;
        return preco.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean vigente(String fim, Instant agora) {
        if (fim == null || fim.isBlank()) return true;
        try {
            return Instant.parse(fim).isAfter(agora);
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    /** Categoria principal do produto, a partir do detalhe (/products/{id}). */
    static Categoria parseCategoria(String json) {
        JsonObject body;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            body = reader.readObject();
        }
        if (!(body.get("data") instanceof JsonObject produto)) return null;
        if (!(produto.get("subcategories") instanceof JsonArray subcategorias)) return null;

        Categoria primeira = null;
        for (JsonValue valor : subcategorias) {
            if (!(valor instanceof JsonObject sub) || !(sub.get("category") instanceof JsonObject cat)) continue;
            String id = cat.getString("id", "");
            String titulo = cat.getString("title", "").trim();
            if (id.isEmpty() || titulo.isEmpty()) continue;

            Categoria categoria = new Categoria(id, titulo);
            if (sub.getBoolean("is_main", false)) return categoria;
            if (primeira == null) primeira = categoria;
        }
        return primeira;
    }
}
