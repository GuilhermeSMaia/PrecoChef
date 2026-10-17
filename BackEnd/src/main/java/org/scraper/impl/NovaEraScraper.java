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
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Scraper do site https://www.supernovaera.com.br (loja de Porto Velho).
 *
 * O Nova Era roda em VTEX e tem uma loja (seller) por cidade. As APIs de busca
 * (/api/catalog_system/pub/products/search e intelligent-search) sempre devolvem o preço
 * da loja padrão (Manaus), então a coleta é feita em duas etapas:
 *   1. lista os produtos pela intelligent-search com o regionId da cidade (sortimento da loja):
 *      GET /api/io/_v/api/intelligent-search/product_search/category-1/{depto}[/category-2/{sub}]?count=50&page=N
 *          &regionId=...&hideUnavailableItems=true
 *   2. consulta o preço e o estoque da loja em lote, simulando um carrinho com o CEP da cidade:
 *      POST /api/checkout/pub/orderForms/simulation
 *
 * A busca não pagina além de 2500 itens; departamentos maiores (ex.: Alimentos) são
 * divididos nas subcategorias da árvore /api/catalog_system/pub/category/tree/3.
 */
@ApplicationScoped
public class NovaEraScraper extends AbstractMercadoScraper {

    private static final Logger LOG = Logger.getLogger(NovaEraScraper.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";
    private static final String CAMINHO_BUSCA = "/api/io/_v/api/intelligent-search/product_search/";
    private static final int ITENS_POR_PAGINA = 50;  // máximo aceito pela API
    private static final int MAX_PAGINAS = 50;       // a API recusa page > 50 (limite de 2500 itens por busca)
    private static final int LIMITE_BUSCA = ITENS_POR_PAGINA * MAX_PAGINAS;
    private static final int ITENS_POR_SIMULACAO = 100;
    private static final int MAX_TENTATIVAS = 3;

    @ConfigProperty(name = "scraping.novaera.nome-mercado", defaultValue = "Nova Era")
    String nomeMercado;

    @ConfigProperty(name = "scraping.novaera.base-url", defaultValue = "https://www.supernovaera.com.br")
    String baseUrl;

    // CEP usado para escolher a loja (preço e estoque); 76801000 = Porto Velho
    @ConfigProperty(name = "scraping.novaera.cep", defaultValue = "76801000")
    String cep;

    @ConfigProperty(name = "scraping.novaera.delay-ms", defaultValue = "500")
    long delayMs;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String getNomeMercado() {
        return nomeMercado;
    }

    // um departamento por URL (raízes da árvore de categorias)
    @Override
    public List<String> getUrls() {
        try {
            return lerArvore(baixar(baseUrl + "/api/catalog_system/pub/category/tree/1")).stream()
                    .map(c -> urlBusca(c.caminho()))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("falha ao ler as categorias do site: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ScrapedProdutoDTO> scrape() throws Exception {
        String regionId = buscarRegionId();
        Map<String, ProdutoBusca> encontrados = new LinkedHashMap<>(); // chave = productId da VTEX
        String ultimoErro = null;

        for (Categoria departamento : lerArvore(baixar(baseUrl + "/api/catalog_system/pub/category/tree/3"))) {
            try {
                int antes = encontrados.size();
                coletarCategoria(departamento, regionId, encontrados);
                LOG.infof("[%s] %d produtos encontrados em %s", nomeMercado, encontrados.size() - antes, departamento.caminho());
            } catch (Exception e) {
                ultimoErro = e.getMessage();
                LOG.errorf("[%s] falha ao coletar %s: %s", nomeMercado, departamento.caminho(), e.getMessage());
                // um departamento com erro não derruba a coleta dos demais
            }
        }

        if (encontrados.isEmpty()) {
            throw new IllegalStateException("nenhum produto encontrado: " + ultimoErro);
        }
        List<ScrapedProdutoDTO> produtos = precificar(new ArrayList<>(encontrados.values()));
        if (produtos.isEmpty()) {
            throw new IllegalStateException("nenhum produto com preço/estoque para o CEP " + cep);
        }
        LOG.infof("[%s] coleta concluída: %d produtos com preço (de %d encontrados)", nomeMercado, produtos.size(), encontrados.size());
        return produtos;
    }

    @Override
    protected List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception {
        String regionId = buscarRegionId();
        Map<String, ProdutoBusca> encontrados = new LinkedHashMap<>();
        for (int pagina = 1; pagina <= MAX_PAGINAS; pagina++) {
            PaginaBusca resposta = parseBusca(baixar(url + parametrosBusca(pagina, regionId)));
            resposta.produtos().forEach(p -> encontrados.putIfAbsent(p.productId(), p));
            if (pagina * ITENS_POR_PAGINA >= resposta.total()) break;
            Thread.sleep(delayMs);
        }
        return precificar(new ArrayList<>(encontrados.values()));
    }

    // ---------------------------------------------------------------- etapa 1: listagem

    private void coletarCategoria(Categoria categoria, String regionId, Map<String, ProdutoBusca> encontrados) throws Exception {
        String url = urlBusca(categoria.caminho());
        PaginaBusca primeira = parseBusca(baixar(url + parametrosBusca(1, regionId)));
        Thread.sleep(delayMs);

        // passou do limite de paginação: coleta cada subcategoria separadamente
        if (primeira.total() > LIMITE_BUSCA && !categoria.filhas().isEmpty()) {
            for (Categoria filha : categoria.filhas()) {
                coletarCategoria(filha, regionId, encontrados);
            }
            return;
        }
        if (primeira.total() > LIMITE_BUSCA) {
            LOG.warnf("[%s] %s tem %d produtos e não tem subcategorias - só os primeiros %d serão coletados",
                    nomeMercado, categoria.caminho(), primeira.total(), LIMITE_BUSCA);
        }

        primeira.produtos().forEach(p -> encontrados.putIfAbsent(p.productId(), p));
        int paginas = Math.min(MAX_PAGINAS, (primeira.total() + ITENS_POR_PAGINA - 1) / ITENS_POR_PAGINA);
        for (int pagina = 2; pagina <= paginas; pagina++) {
            try {
                parseBusca(baixar(url + parametrosBusca(pagina, regionId))).produtos()
                        .forEach(p -> encontrados.putIfAbsent(p.productId(), p));
            } catch (Exception e) {
                // uma página com erro não derruba o resto da categoria
                LOG.errorf("[%s] falha na página %d de %s: %s", nomeMercado, pagina, categoria.caminho(), e.getMessage());
            }
            Thread.sleep(delayMs);
        }
    }

    // caminho "alimentos/cafe" -> .../product_search/category-1/alimentos/category-2/cafe
    private String urlBusca(List<String> caminho) {
        StringBuilder url = new StringBuilder(baseUrl).append(CAMINHO_BUSCA);
        for (int i = 0; i < caminho.size(); i++) {
            if (i > 0) url.append('/');
            url.append("category-").append(i + 1).append('/').append(caminho.get(i));
        }
        return url.toString();
    }

    // só o regionId não filtra nada; com hideUnavailableItems a busca devolve só o sortimento da loja
    // (~40% do catálogo, sem perder nenhum item que tenha preço na simulação)
    private static String parametrosBusca(int pagina, String regionId) {
        return "?count=" + ITENS_POR_PAGINA + "&page=" + pagina + "&locale=pt-BR&hideUnavailableItems=true"
                + "&regionId=" + URLEncoder.encode(regionId, StandardCharsets.UTF_8);
    }

    // a loja que atende o CEP; a VTEX devolve um regionId que filtra o sortimento da busca
    private String buscarRegionId() throws Exception {
        String json = baixar(baseUrl + "/api/checkout/pub/regions?country=BRA&postalCode="
                + URLEncoder.encode(cep.replaceAll("\\D", ""), StandardCharsets.UTF_8));
        String regionId = parseRegionId(json);
        if (regionId == null) {
            throw new IllegalStateException("nenhuma loja atende o CEP " + cep + " (scraping.novaera.cep)");
        }
        return regionId;
    }

    // ---------------------------------------------------------------- etapa 2: preços da loja

    private List<ScrapedProdutoDTO> precificar(List<ProdutoBusca> encontrados) throws Exception {
        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        int falhas = 0;

        for (int inicio = 0; inicio < encontrados.size(); inicio += ITENS_POR_SIMULACAO) {
            List<ProdutoBusca> lote = encontrados.subList(inicio, Math.min(inicio + ITENS_POR_SIMULACAO, encontrados.size()));
            try {
                Map<String, BigDecimal> precos = parseSimulacao(simular(lote));
                for (ProdutoBusca p : lote) {
                    BigDecimal preco = precos.get(p.itemId());
                    if (preco != null) produtos.add(p.paraDto(preco));
                }
            } catch (Exception e) {
                falhas++;
                LOG.errorf("[%s] falha ao consultar preços (itens %d a %d): %s",
                        nomeMercado, inicio, inicio + lote.size(), e.getMessage());
            }
            Thread.sleep(delayMs);
        }

        if (falhas > 0) LOG.warnf("[%s] %d lotes de preço falharam", nomeMercado, falhas);
        return produtos;
    }

    private String simular(List<ProdutoBusca> lote) throws Exception {
        JsonArrayBuilder itens = Json.createArrayBuilder();
        for (ProdutoBusca p : lote) {
            itens.add(Json.createObjectBuilder().add("id", p.itemId()).add("quantity", 1).add("seller", "1"));
        }
        String corpo = Json.createObjectBuilder()
                .add("items", itens)
                .add("postalCode", cep.replaceAll("\\D", ""))
                .add("country", "BRA")
                .build().toString();

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/checkout/pub/orderForms/simulation?sc=1"))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();

        return enviar(request, "simulação de preços");
    }

    private String baixar(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        return enviar(request, url);
    }

    // a API da VTEX devolve HTTP 500 de vez em quando; tenta de novo algumas vezes antes de desistir
    private String enviar(HttpRequest request, String descricao) throws Exception {
        Exception ultimoErro = null;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            if (tentativa > 1) Thread.sleep(2000L * (tentativa - 1));
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                // a busca do catálogo responde 206 (Partial Content) com a página pedida
                if (response.statusCode() == 200 || response.statusCode() == 206) return response.body();
                ultimoErro = new IllegalStateException("HTTP " + response.statusCode() + " em " + descricao);
                if (response.statusCode() < 500 && response.statusCode() != 429) break; // erro do pedido, não adianta repetir
            } catch (IOException e) {
                ultimoErro = e;
            }
        }
        throw ultimoErro;
    }

    // ---------------------------------------------------------------- leitura do JSON

    record Categoria(List<String> caminho, List<Categoria> filhas) {}

    record ProdutoBusca(String productId, String itemId, String nome, String categoria, String marca, String ean, boolean porKg) {
        ScrapedProdutoDTO paraDto(BigDecimal preco) {
            // vendido por kg: o preço consultado é o do quilo
            MedidaParser.Medida medida = porKg ? new MedidaParser.Medida(1, "kg") : MedidaParser.parse(nome);
            ScrapedProdutoDTO dto = new ScrapedProdutoDTO(nome, categoria, medida.unidade(), medida.quantidade(), preco);
            dto.marca = marca;
            dto.codigoBarras = ean;
            return dto;
        }
    }

    record PaginaBusca(List<ProdutoBusca> produtos, int total) {}

    /** Árvore de /api/catalog_system/pub/category/tree/N; o caminho sai da url ("https://.../alimentos/cafe"). */
    List<Categoria> lerArvore(String json) {
        List<Categoria> categorias = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            lerCategorias(reader.readArray(), categorias);
        }
        return categorias;
    }

    private void lerCategorias(JsonArray lista, List<Categoria> destino) {
        for (JsonValue valor : lista) {
            JsonObject c = valor.asJsonObject();
            String url = c.getString("url", "");
            if (!url.startsWith(baseUrl)) continue;

            List<String> caminho = new ArrayList<>();
            for (String parte : url.substring(baseUrl.length()).split("/")) {
                if (!parte.isBlank()) caminho.add(parte);
            }
            if (caminho.isEmpty()) continue;

            List<Categoria> filhas = new ArrayList<>();
            JsonArray children = c.getJsonArray("children");
            if (children != null) lerCategorias(children, filhas);
            destino.add(new Categoria(caminho, filhas));
        }
    }

    static String parseRegionId(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            for (JsonValue valor : reader.readArray()) {
                JsonObject regiao = valor.asJsonObject();
                JsonArray sellers = regiao.getJsonArray("sellers");
                if (sellers != null && !sellers.isEmpty()) return regiao.getString("id", null);
            }
        }
        return null;
    }

    static PaginaBusca parseBusca(String json) {
        JsonObject body;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            body = reader.readObject();
        }

        JsonArray lista = body.containsKey("products") && !body.isNull("products")
                ? body.getJsonArray("products")
                : JsonValue.EMPTY_JSON_ARRAY;

        List<ProdutoBusca> produtos = new ArrayList<>();
        for (JsonValue valor : lista) {
            JsonObject produto = valor.asJsonObject();
            String nome = produto.getString("productName", "").replaceAll("\\s+", " ").trim();
            JsonArray items = produto.getJsonArray("items");
            if (nome.isEmpty() || items == null || items.isEmpty()) continue;

            JsonObject item = items.getJsonObject(0);
            String itemId = item.getString("itemId", "");
            if (itemId.isEmpty()) continue;

            String marca = produto.getString("brand", "").trim();
            boolean porKg = item.getString("measurementUnit", "").equalsIgnoreCase("kg");
            produtos.add(new ProdutoBusca(produto.getString("productId", itemId), itemId, nome,
                    departamento(produto), marca.isEmpty() ? null : marca, item.getString("ean", null), porKg));
        }

        int total = body.get("recordsFiltered") instanceof JsonNumber n ? n.intValue() : lista.size();
        return new PaginaBusca(produtos, total);
    }

    // a categoria principal vem primeiro; depois podem vir coleções promocionais:
    // ["/Bebidas/Cervejas/", "/Bebidas/", "/Destaques/", "/Destaques/Encarte/"] -> "Bebidas"
    private static String departamento(JsonObject produto) {
        JsonArray categorias = produto.getJsonArray("categories");
        if (categorias == null || categorias.isEmpty()) return null;

        String caminho = categorias.getString(0, "");
        for (String parte : caminho.split("/")) {
            if (!parte.isBlank()) return parte.trim();
        }
        return null;
    }

    /**
     * Preço por itemId (SKU) na loja do CEP; itens sem estoque ficam de fora.
     * Valores em centavos. Em itens vendidos por kg a VTEX simula uma fração (unitMultiplier, ex.: 0.2 kg):
     * "price" é o preço do kg e "sellingPrice" o da fração já com desconto.
     */
    static Map<String, BigDecimal> parseSimulacao(String json) {
        JsonObject body;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            body = reader.readObject();
        }

        Map<String, BigDecimal> precos = new HashMap<>();
        JsonArray items = body.getJsonArray("items");
        if (items == null) return precos;

        for (JsonValue valor : items) {
            JsonObject item = valor.asJsonObject();
            if (!"available".equals(item.getString("availability", ""))) continue;

            BigDecimal precoCheio = centavos(item, "price");
            BigDecimal precoFinal = centavos(item, "sellingPrice");
            BigDecimal multiplicador = item.get("unitMultiplier") instanceof JsonNumber n ? n.bigDecimalValue() : BigDecimal.ONE;
            if (precoCheio == null || precoFinal == null || precoFinal.signum() <= 0 || multiplicador.signum() <= 0) continue;

            BigDecimal preco = precoFinal.divide(multiplicador, 2, RoundingMode.HALF_UP);
            // a fração é arredondada em centavos (0,2 kg x 13,99 = 2,79 -> 13,95/kg); sem desconto, vale o preço cheio
            if (multiplicador.compareTo(BigDecimal.ONE) != 0
                    && precoFinal.compareTo(precoCheio.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP).subtract(new BigDecimal("0.01"))) >= 0) {
                preco = precoCheio;
            }
            precos.put(item.getString("id", ""), preco.setScale(2, RoundingMode.HALF_UP));
        }
        return precos;
    }

    private static BigDecimal centavos(JsonObject obj, String chave) {
        return obj.get(chave) instanceof JsonNumber n ? n.bigDecimalValue().movePointLeft(2) : null;
    }
}
