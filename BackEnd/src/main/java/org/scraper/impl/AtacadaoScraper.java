package org.scraper.impl;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 * Scraper do site https://www.atacadao.com.br
 *
 * O Atacadão roda em VTEX (FastStore). Em vez de ler o HTML, usa a API pública de
 * busca da própria loja, que devolve JSON com nome, marca, departamento e preço:
 *   GET /api/io/_v/api/intelligent-search/product_search/category-1/{departamento}?count=50&page=N
 *
 * Não usar a rota /_next/data/{buildId}/... : o buildId muda a cada deploy do site.
 */
@ApplicationScoped
public class AtacadaoScraper extends AbstractMercadoScraper {

    private static final Logger LOG = Logger.getLogger(AtacadaoScraper.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";
    private static final String CAMINHO_BUSCA = "/api/io/_v/api/intelligent-search/product_search/category-1/";
    private static final int ITENS_POR_PAGINA = 50; // máximo aceito pela API
    private static final int MAX_PAGINAS = 50;      // a VTEX não pagina além de ~2500 itens por busca

    @ConfigProperty(name = "scraping.atacadao.nome-mercado", defaultValue = "Atacadão")
    String nomeMercado;

    @ConfigProperty(name = "scraping.atacadao.base-url", defaultValue = "https://www.atacadao.com.br")
    String baseUrl;

    // slugs dos departamentos do site (atacadao.com.br/{slug})
    @ConfigProperty(name = "scraping.atacadao.departamentos", defaultValue =
            "mercearia,carnes-aves-e-peixes,hortifruti,padaria-e-matinais,frios-e-congelados,bebidas,limpeza,higiene-e-perfumaria")
    List<String> departamentos;

    @ConfigProperty(name = "scraping.atacadao.delay-ms", defaultValue = "1000")
    long delayMs;

    // preços variam por filial; vazio = região padrão do site
    @ConfigProperty(name = "scraping.atacadao.region-id")
    Optional<String> regionId;

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
        return departamentos.stream().map(d -> baseUrl + CAMINHO_BUSCA + d.trim()).toList();
    }

    @Override
    public List<ScrapedProdutoDTO> scrape() throws Exception {
        Map<String, ScrapedProdutoDTO> produtos = new LinkedHashMap<>(); // chave = productId da VTEX
        String ultimoErro = null;

        for (String url : getUrls()) {
            try {
                int antes = produtos.size();
                coletarDepartamento(url, produtos);
                LOG.infof("[%s] %d produtos coletados em %s", nomeMercado, produtos.size() - antes, url);
            } catch (Exception e) {
                ultimoErro = e.getMessage();
                LOG.errorf("[%s] falha ao coletar %s: %s", nomeMercado, url, e.getMessage());
                // um departamento com erro não derruba a coleta dos demais
            }
        }

        if (produtos.isEmpty()) {
            throw new IllegalStateException("nenhum produto coletado: " + ultimoErro);
        }
        LOG.infof("[%s] coleta concluída: %d produtos", nomeMercado, produtos.size());
        return new ArrayList<>(produtos.values());
    }

    @Override
    protected List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception {
        Map<String, ScrapedProdutoDTO> produtos = new LinkedHashMap<>();
        coletarDepartamento(url, produtos);
        return new ArrayList<>(produtos.values());
    }

    private void coletarDepartamento(String url, Map<String, ScrapedProdutoDTO> produtos) throws Exception {
        for (int pagina = 1; pagina <= MAX_PAGINAS; pagina++) {
            Resposta resposta = parseResposta(baixar(url, pagina));
            resposta.produtos().forEach(produtos::putIfAbsent);

            if (resposta.quantidadeNaPagina() < ITENS_POR_PAGINA) break; // página incompleta = última
            Thread.sleep(delayMs); // evita sobrecarregar/ser bloqueado pelo site
        }
    }

    private String baixar(String url, int pagina) throws Exception {
        StringBuilder query = new StringBuilder()
                .append("?count=").append(ITENS_POR_PAGINA)
                .append("&page=").append(pagina)
                .append("&locale=pt-BR");
        regionId.filter(r -> !r.isBlank())
                .ifPresent(r -> query.append("&regionId=").append(URLEncoder.encode(r.trim(), StandardCharsets.UTF_8)));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url + query))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " em " + url + " (página " + pagina + ")");
        }
        return response.body();
    }

    // quantidadeNaPagina conta também os produtos descartados (sem preço/estoque), para a paginação
    record Resposta(Map<String, ScrapedProdutoDTO> produtos, int quantidadeNaPagina) {}

    static Resposta parseResposta(String json) {
        Map<String, ScrapedProdutoDTO> produtos = new LinkedHashMap<>();
        JsonObject body;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            body = reader.readObject();
        }

        JsonArray lista = body.containsKey("products") && !body.isNull("products")
                ? body.getJsonArray("products")
                : JsonValue.EMPTY_JSON_ARRAY;

        for (JsonValue valor : lista) {
            JsonObject produto = valor.asJsonObject();
            ScrapedProdutoDTO dto = converter(produto);
            if (dto != null) produtos.put(produto.getString("productId", dto.nome), dto);
        }
        return new Resposta(produtos, lista.size());
    }

    // produto VTEX -> DTO; devolve null se não houver preço ou estoque
    private static ScrapedProdutoDTO converter(JsonObject produto) {
        String nome = produto.getString("productName", "").trim();
        JsonArray items = produto.getJsonArray("items");
        if (nome.isEmpty() || items == null || items.isEmpty()) return null;

        JsonArray sellers = items.getJsonObject(0).getJsonArray("sellers");
        if (sellers == null || sellers.isEmpty()) return null;

        JsonObject oferta = sellers.getJsonObject(0).getJsonObject("commertialOffer");
        if (oferta == null) return null;

        JsonNumber valor = oferta.getJsonNumber("Price");
        if (valor == null || valor.doubleValue() <= 0 || oferta.getInt("AvailableQuantity", 0) <= 0) return null;
        BigDecimal preco = valor.bigDecimalValue().setScale(2, RoundingMode.HALF_UP);

        MedidaParser.Medida medida = MedidaParser.parse(nome);
        ScrapedProdutoDTO dto = new ScrapedProdutoDTO(nome, departamento(produto), medida.unidade(), medida.quantidade(), preco);
        String marca = produto.getString("brand", "").trim();
        dto.marca = marca.isEmpty() ? null : marca;
        return dto;
    }

    // "categories" vem do mais específico ao mais geral: ["/Mercearia/Arroz/", "/Mercearia/"]
    private static String departamento(JsonObject produto) {
        JsonArray categorias = produto.getJsonArray("categories");
        if (categorias == null || categorias.isEmpty()) return null;

        String caminho = categorias.getString(categorias.size() - 1, "");
        for (String parte : caminho.split("/")) {
            if (!parte.isBlank()) return parte.trim();
        }
        return null;
    }
}
