package org.scraper.impl;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Scraper do Supermercado Meta 21 (https://supermercadometa21.instabuy.app.br).
 *
 * A loja roda na plataforma Instabuy (Next.js). A API pública da plataforma (api.ibecom.com.br)
 * bloqueia o IP (HTTP 429) depois de poucas requisições e não deixa paginar a loja inteira, então
 * a coleta usa o próprio site, que não tem esse limite:
 *   GET /sitemap.xml   lista as páginas de todos os produtos (/p/{slug})
 *   GET /p/{slug}      página do produto, montada no servidor com o JSON da API embutido
 *                      (self.__next_f.push(...) com "initialData":{"data":{...}})
 * O site não informa código de barras.
 */
@ApplicationScoped
public class Meta21Scraper extends AbstractMercadoScraper {

    private static final Logger LOG = Logger.getLogger(Meta21Scraper.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";
    private static final int MAX_TENTATIVAS = 3;
    private static final int MAX_BLOQUEIOS = 4;          // esperas seguidas após HTTP 429 antes de desistir
    private static final int MAX_FALHAS_SEGUIDAS = 20;   // site fora do ar: para em vez de tentar os 5 mil produtos
    private static final int LOG_A_CADA = 500;

    private static final Pattern LOC_PRODUTO = Pattern.compile("<loc>\\s*([^<\\s]+/p/[^<\\s]+)\\s*</loc>");
    private static final Pattern PUSH_NEXT = Pattern.compile("self\\.__next_f\\.push\\((\\[.*?\\])\\)</script>", Pattern.DOTALL);
    private static final String MARCA_DADOS = "\"initialData\":{\"data\":";

    @ConfigProperty(name = "scraping.meta21.nome-mercado", defaultValue = "Meta 21")
    String nomeMercado;

    @ConfigProperty(name = "scraping.meta21.site-url", defaultValue = "https://supermercadometa21.instabuy.app.br")
    String siteUrl;

    // pausa entre uma página de produto e outra (são ~5 mil páginas por coleta); o site
    // responde 429 acima de ~60 páginas por minuto
    @ConfigProperty(name = "scraping.meta21.delay-ms", defaultValue = "1000")
    long delayMs;

    // espera após um HTTP 429 antes de repetir a mesma requisição
    @ConfigProperty(name = "scraping.meta21.espera-bloqueio-ms", defaultValue = "120000")
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
        return List.of(siteUrl + "/sitemap.xml");
    }

    @Override
    public List<ScrapedProdutoDTO> scrape() throws Exception {
        List<String> urls = parseSitemap(baixar(siteUrl + "/sitemap.xml"));
        if (urls.isEmpty()) {
            throw new IllegalStateException("nenhum produto no sitemap (verifique scraping.meta21.site-url)");
        }
        LOG.infof("[%s] %d produtos no sitemap", nomeMercado, urls.size());

        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        int lidas = 0;
        int falhasSeguidas = 0;
        for (String url : urls) {
            Thread.sleep(delayMs);
            try {
                ScrapedProdutoDTO dto = scrapeProduto(url);
                if (dto != null) produtos.add(dto);
                falhasSeguidas = 0;
            } catch (BloqueadoException e) {
                // salva o que já foi coletado em vez de perder tudo
                LOG.errorf("[%s] %s - coleta interrompida (%d de %d páginas)", nomeMercado, e.getMessage(), lidas, urls.size());
                break;
            } catch (Exception e) {
                LOG.errorf("[%s] falha no produto %s: %s", nomeMercado, url, e.getMessage());
                if (++falhasSeguidas >= MAX_FALHAS_SEGUIDAS) {
                    LOG.errorf("[%s] %d falhas seguidas - coleta interrompida (%d de %d páginas)",
                            nomeMercado, falhasSeguidas, lidas, urls.size());
                    break;
                }
            }
            if (++lidas % LOG_A_CADA == 0) {
                LOG.infof("[%s] %d de %d páginas lidas, %d produtos com preço", nomeMercado, lidas, urls.size(), produtos.size());
            }
        }
        if (produtos.isEmpty()) {
            throw new IllegalStateException("nenhum produto coletado");
        }
        LOG.infof("[%s] coleta concluída: %d produtos com preço (de %d páginas lidas)", nomeMercado, produtos.size(), lidas);
        return produtos;
    }

    @Override
    protected List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception {
        ScrapedProdutoDTO dto = scrapeProduto(url);
        return dto == null ? List.of() : List.of(dto);
    }

    private ScrapedProdutoDTO scrapeProduto(String url) throws Exception {
        Produto produto = parseProduto(baixar(url));
        if (produto == null) throw new IllegalStateException("dados do produto não encontrados na página");
        return produto.item().paraDto(produto.categoria());
    }

    String baixar(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "text/html,application/xml")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        Exception ultimoErro = null;
        long espera = 0;
        int bloqueios = 0;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            if (espera > 0) Thread.sleep(espera);
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return response.body();
                if (response.statusCode() == 429) {
                    // bloqueio temporário por excesso de requisições: espera e repete a mesma página,
                    // sem gastar tentativa, para a coleta continuar de onde parou
                    if (++bloqueios > MAX_BLOQUEIOS) {
                        throw new BloqueadoException("site bloqueou por excesso de requisições (HTTP 429) em " + url);
                    }
                    espera = esperaBloqueioMs;
                    LOG.warnf("[%s] HTTP 429 (%d/%d) - aguardando %ds antes de tentar de novo",
                            nomeMercado, bloqueios, MAX_BLOQUEIOS, espera / 1000);
                    tentativa--;
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

    // o site continua bloqueando mesmo após esperar: insistir só prolonga o bloqueio
    static class BloqueadoException extends IllegalStateException {
        BloqueadoException(String mensagem) {
            super(mensagem);
        }
    }

    // ---------------------------------------------------------------- leitura das páginas

    record Produto(Item item, String categoria) {}

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

    /** URLs das páginas de produto (/p/...) do sitemap.xml, sem repetição. */
    static List<String> parseSitemap(String xml) {
        Set<String> urls = new LinkedHashSet<>();
        Matcher m = LOC_PRODUTO.matcher(xml);
        while (m.find()) urls.add(m.group(1).replace("&amp;", "&"));
        return new ArrayList<>(urls);
    }

    static Produto parseProduto(String html) {
        return parseProduto(html, Instant.now());
    }

    /**
     * O Next.js manda os dados da página em pedaços de texto: self.__next_f.push([1,"..."]).
     * Juntando os pedaços, o produto aparece como (resumido):
     * "initialData":{"data":{"id":"6a51...","name":"Arroz Bernardo 1Kg Tipo 1","unit_type":"UNI",
     *   "price_config":{"price":4.59},"stock":{"has_available_stock":true},
     *   "subcategories":[{"title":"Arroz","is_main":true,"category":{"title":"Alimentos Básicos"}}]}}
     */
    static Produto parseProduto(String html, Instant agora) {
        StringBuilder payload = new StringBuilder();
        Matcher m = PUSH_NEXT.matcher(html);
        while (m.find()) {
            try (JsonReader reader = Json.createReader(new StringReader(m.group(1)))) {
                JsonArray push = reader.readArray();
                if (push.size() > 1 && push.get(1) instanceof JsonString texto) payload.append(texto.getString());
            } catch (RuntimeException e) {
                // pedaço que não é texto (ex.: [0] de inicialização): ignora
            }
        }

        int inicio = payload.indexOf(MARCA_DADOS);
        if (inicio < 0) return null;
        String json = objetoJson(payload, inicio + MARCA_DADOS.length());
        if (json == null) return null;

        JsonObject p;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            p = reader.readObject();
        }
        Item item = item(p, agora);
        return item == null ? null : new Produto(item, categoria(p));
    }

    // o objeto JSON que começa em "inicio", contando chaves fora de strings
    private static String objetoJson(CharSequence texto, int inicio) {
        if (inicio >= texto.length() || texto.charAt(inicio) != '{') return null;
        int nivel = 0;
        boolean emString = false;
        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (emString) {
                if (c == '\\') i++;
                else if (c == '"') emString = false;
            } else if (c == '"') {
                emString = true;
            } else if (c == '{') {
                nivel++;
            } else if (c == '}' && --nivel == 0) {
                return texto.subSequence(inicio, i + 1).toString();
            }
        }
        return null;
    }

    static Item item(JsonObject p, Instant agora) {
        String id = p.getString("id", "");
        String nome = p.getString("name", "").replaceAll("\\s+", " ").trim();
        if (id.isEmpty() || nome.isEmpty()) return null;

        String marca = p.getString("brand", "").trim();
        boolean porKg = p.getString("unit_type", "").equalsIgnoreCase("KG");
        return new Item(id, nome, marca.isEmpty() ? null : marca, porKg, preco(p, agora));
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

    /** Categoria principal do produto (a da subcategoria marcada is_main; senão, a primeira). */
    static String categoria(JsonObject produto) {
        if (!(produto.get("subcategories") instanceof JsonArray subcategorias)) return null;
        String primeira = null;
        for (JsonValue valor : subcategorias) {
            if (!(valor instanceof JsonObject sub) || !(sub.get("category") instanceof JsonObject cat)) continue;
            String titulo = cat.getString("title", "").trim();
            if (titulo.isEmpty()) continue;
            if (sub.getBoolean("is_main", false)) return titulo;
            if (primeira == null) primeira = titulo;
        }
        return primeira;
    }
}
