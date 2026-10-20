package org.scraper.impl;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.scraper.AbstractMercadoScraper;
import org.scraper.MedidaParser;
import org.scraper.dto.ScrapedProdutoDTO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Scraper do site https://www.atacadao.com.br
 *
 * O Atacadão roda em VTEX (FastStore) e tem uma loja (seller) por filial. A coleta é feita em duas etapas:
 *   1. lista os produtos da filial pela busca da própria loja (nome, marca, departamento, EAN):
 *      GET /api/io/_v/api/intelligent-search/product_search/category-1/{departamento}?count=50&page=N
 *          &regionId=...&hideUnavailableItems=true
 *   2. consulta preço e estoque na filial que atende scraping.atacadao.cep, simulando um carrinho em lote:
 *      POST /api/checkout/pub/orderForms/simulation
 * O preço da busca é sempre o genérico do site (seller "1", ~ preço de SP), por isso não é usado.
 *
 * A loja de Porto Velho não vende pelo site. O CEP padrão é o de Cuiabá, cuja tabela bate com os encartes
 * de Rondônia (preço idêntico em 20 de 29 itens comparados em out/2026; Manaus/Belém variam 24-28%).
 *
 * Não usar a rota /_next/data/{buildId}/... : o buildId muda a cada deploy do site.
 */
@ApplicationScoped
public class AtacadaoScraper extends AbstractMercadoScraper {

    private static final Logger LOG = Logger.getLogger(AtacadaoScraper.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";
    private static final String CAMINHO_BUSCA = "/api/io/_v/api/intelligent-search/product_search/category-1/";
    private static final int ITENS_POR_PAGINA = 50;     // máximo aceito pela API
    private static final int MAX_PAGINAS = 50;          // a VTEX não pagina além de ~2500 itens por busca
    private static final int ITENS_POR_SIMULACAO = 100;
    private static final int MAX_TENTATIVAS = 3;

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

    // CEP que escolhe a filial (preço e estoque); 78005000 = Cuiabá, usada como referência para Porto Velho
    @ConfigProperty(name = "scraping.atacadao.cep", defaultValue = "78005000")
    String cep;

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
        Filial filial = buscarFilial();
        Map<String, ProdutoBusca> encontrados = new LinkedHashMap<>(); // chave = productId da VTEX
        String ultimoErro = null;

        for (String url : getUrls()) {
            try {
                int antes = encontrados.size();
                coletarDepartamento(url, filial.regionId(), encontrados);
                LOG.infof("[%s] %d produtos encontrados em %s", nomeMercado, encontrados.size() - antes, url);
            } catch (Exception e) {
                ultimoErro = e.getMessage();
                LOG.errorf("[%s] falha ao coletar %s: %s", nomeMercado, url, e.getMessage());
                // um departamento com erro não derruba a coleta dos demais
            }
        }

        if (encontrados.isEmpty()) {
            throw new IllegalStateException("nenhum produto encontrado: " + ultimoErro);
        }
        List<ScrapedProdutoDTO> produtos = precificar(new ArrayList<>(encontrados.values()), filial.seller());
        if (produtos.isEmpty()) {
            throw new IllegalStateException("nenhum produto com preço/estoque na filial " + filial.seller());
        }
        LOG.infof("[%s] coleta concluída: %d de %d produtos com preço na filial %s",
                nomeMercado, produtos.size(), encontrados.size(), filial.seller());
        return produtos;
    }

    @Override
    protected List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception {
        Filial filial = buscarFilial();
        Map<String, ProdutoBusca> encontrados = new LinkedHashMap<>();
        coletarDepartamento(url, filial.regionId(), encontrados);
        return precificar(new ArrayList<>(encontrados.values()), filial.seller());
    }

    // ---------------------------------------------------------------- etapa 1: catálogo

    private void coletarDepartamento(String url, String regionId, Map<String, ProdutoBusca> encontrados) throws Exception {
        for (int pagina = 1; pagina <= MAX_PAGINAS; pagina++) {
            PaginaBusca resposta = parseBusca(baixar(url + parametrosBusca(pagina, regionId)));
            resposta.produtos().forEach(encontrados::putIfAbsent);

            if (resposta.quantidadeNaPagina() < ITENS_POR_PAGINA) break; // página incompleta = última
            Thread.sleep(delayMs); // evita sobrecarregar/ser bloqueado pelo site
        }
    }

    // regionId + hideUnavailableItems: só o sortimento da filial (sem isso a busca lista o catálogo
    // nacional, ~48 mil itens, e o limite de 2500 por departamento cortaria produtos da filial)
    private static String parametrosBusca(int pagina, String regionId) {
        return "?count=" + ITENS_POR_PAGINA + "&page=" + pagina + "&locale=pt-BR&hideUnavailableItems=true"
                + "&regionId=" + URLEncoder.encode(regionId, StandardCharsets.UTF_8);
    }

    // filial que atende o CEP; sem nenhuma, aborta em vez de gravar o preço genérico do site
    private Filial buscarFilial() throws Exception {
        Filial filial = parseFilial(baixar(baseUrl + "/api/checkout/pub/regions?country=BRA&postalCode="
                + URLEncoder.encode(cep.replaceAll("\\D", ""), StandardCharsets.UTF_8)));
        if (filial == null) {
            throw new IllegalStateException("nenhuma filial do Atacadão atende o CEP " + cep
                    + " pelo site (scraping.atacadao.cep); nada será gravado");
        }
        LOG.infof("[%s] preços da filial %s (CEP %s)", nomeMercado, filial.seller(), cep);
        return filial;
    }

    // ---------------------------------------------------------------- etapa 2: preços da filial

    private List<ScrapedProdutoDTO> precificar(List<ProdutoBusca> encontrados, String seller) throws Exception {
        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        int falhas = 0;

        for (int inicio = 0; inicio < encontrados.size(); inicio += ITENS_POR_SIMULACAO) {
            List<ProdutoBusca> lote = encontrados.subList(inicio, Math.min(inicio + ITENS_POR_SIMULACAO, encontrados.size()));
            try {
                // mesmo formato de resposta do Nova Era (VTEX)
                Map<String, BigDecimal> precos = NovaEraScraper.parseSimulacao(simular(lote, seller));
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

    private String simular(List<ProdutoBusca> lote, String seller) throws Exception {
        JsonArrayBuilder itens = Json.createArrayBuilder();
        for (ProdutoBusca p : lote) {
            itens.add(Json.createObjectBuilder().add("id", p.itemId()).add("quantity", 1).add("seller", seller));
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

    record ProdutoBusca(String itemId, String nome, String categoria, String marca, String ean) {
        ScrapedProdutoDTO paraDto(BigDecimal preco) {
            MedidaParser.Medida medida = MedidaParser.parse(nome);
            ScrapedProdutoDTO dto = new ScrapedProdutoDTO(nome, categoria, medida.unidade(), medida.quantidade(), preco);
            dto.marca = marca;
            dto.codigoBarras = ean;
            return dto;
        }
    }

    // quantidadeNaPagina conta também os produtos descartados, para a paginação
    record PaginaBusca(Map<String, ProdutoBusca> produtos, int quantidadeNaPagina) {}

    record Filial(String regionId, String seller) {}

    // primeira filial da primeira região com filiais; null se nenhuma atende o CEP
    static Filial parseFilial(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            for (JsonValue valor : reader.readArray()) {
                JsonObject regiao = valor.asJsonObject();
                JsonArray sellers = regiao.getJsonArray("sellers");
                if (sellers != null && !sellers.isEmpty()) {
                    return new Filial(regiao.getString("id", ""), sellers.getJsonObject(0).getString("id", null));
                }
            }
        }
        return null;
    }

    static PaginaBusca parseBusca(String json) {
        Map<String, ProdutoBusca> produtos = new LinkedHashMap<>();
        JsonObject body;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            body = reader.readObject();
        }

        JsonArray lista = body.containsKey("products") && !body.isNull("products")
                ? body.getJsonArray("products")
                : JsonValue.EMPTY_JSON_ARRAY;

        for (JsonValue valor : lista) {
            JsonObject produto = valor.asJsonObject();
            ProdutoBusca p = converter(produto);
            if (p != null) produtos.put(produto.getString("productId", p.itemId()), p);
        }
        return new PaginaBusca(produtos, lista.size());
    }

    // produto VTEX -> dados do catálogo; null se faltar nome ou SKU
    private static ProdutoBusca converter(JsonObject produto) {
        String nome = produto.getString("productName", "").trim();
        JsonArray items = produto.getJsonArray("items");
        if (nome.isEmpty() || items == null || items.isEmpty()) return null;

        JsonObject item = items.getJsonObject(0);
        String itemId = item.getString("itemId", "");
        if (itemId.isEmpty()) return null;

        String marca = produto.getString("brand", "").trim();
        return new ProdutoBusca(itemId, nome, departamento(produto), marca.isEmpty() ? null : marca, item.getString("ean", null));
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
