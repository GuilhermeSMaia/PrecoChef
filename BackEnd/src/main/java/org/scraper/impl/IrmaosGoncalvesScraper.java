package org.scraper.impl;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.scraper.AbstractMercadoScraper;
import org.scraper.MedidaParser;
import org.scraper.dto.ScrapedProdutoDTO;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Scraper do site https://www.irmaosgoncalves.com.br
 *
 * Usa a API de busca do próprio site:
 *   GET /api/produto/pesquisar?categoria=/&pagina=N&janela=true
 *
 * Com categoria=/ a API devolve o catálogo inteiro, paginado (campo "paginas").
 * Cada produto já vem com o departamento (ex.: "Mercearia", "Bebidas"), que é
 * usado como Categoria do produto no banco.
 */
@ApplicationScoped
public class IrmaosGoncalvesScraper extends AbstractMercadoScraper {

    private static final Logger LOG = Logger.getLogger(IrmaosGoncalvesScraper.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";
    private static final int MAX_FALHAS_SEGUIDAS = 5;

    @ConfigProperty(name = "scraping.irmaosgoncalves.nome-mercado", defaultValue = "Irmãos Gonçalves")
    String nomeMercado;

    @ConfigProperty(name = "scraping.irmaosgoncalves.base-url", defaultValue = "https://www.irmaosgoncalves.com.br")
    String baseUrl;

    // "/" = todos os produtos. Para coletar só um departamento: "/mercearia", "/bebidas"...
    @ConfigProperty(name = "scraping.irmaosgoncalves.categoria", defaultValue = "/")
    String categoriaBusca;

    // limite de segurança; o total real vem do campo "paginas" da resposta
    @ConfigProperty(name = "scraping.irmaosgoncalves.max-paginas", defaultValue = "2000")
    int maxPaginas;

    @ConfigProperty(name = "scraping.irmaosgoncalves.delay-ms", defaultValue = "500")
    long delayMs;

    @ConfigProperty(name = "scraping.irmaosgoncalves.cookie")
    Optional<String> cookieManual;

    @Override
    public String getNomeMercado() {
        return nomeMercado;
    }

    @Override
    public List<String> getUrls() {
        return List.of(urlPagina(1));
    }

    @Override
    public List<ScrapedProdutoDTO> scrape() throws Exception {
        Map<String, ScrapedProdutoDTO> produtos = new LinkedHashMap<>(); // chave = id do produto no site
        int totalPaginas = maxPaginas;
        int falhasSeguidas = 0;
        String ultimoErro = null;

        // começa em 0 caso a API seja indexada a partir de zero; se não for, os itens repetidos são descartados pelo id
        for (int pagina = 0; pagina <= Math.min(totalPaginas, maxPaginas); pagina++) {
            try {
                Resposta resposta = parseResposta(baixar(urlPagina(pagina)));
                falhasSeguidas = 0;

                if (resposta.paginas() > 0) totalPaginas = resposta.paginas();
                resposta.produtos().forEach((id, p) -> produtos.putIfAbsent(id, p));

                if (resposta.produtos().isEmpty() && pagina >= 1) {
                    // página válida sem produtos = fim da listagem (ou categoria inexistente)
                    if (produtos.isEmpty()) ultimoErro = "a API respondeu sem produtos (verifique scraping.irmaosgoncalves.categoria) - "
                            + urlPagina(pagina);
                    break;
                }

                if (pagina % 50 == 0) {
                    LOG.infof("[%s] página %d/%d - %d produtos até agora", nomeMercado, pagina, totalPaginas, produtos.size());
                }
            } catch (Exception e) {
                ultimoErro = e.getMessage();
                LOG.errorf("[%s] falha ao coletar página %d: %s", nomeMercado, pagina, e.getMessage());
                if (++falhasSeguidas >= MAX_FALHAS_SEGUIDAS) {
                    LOG.errorf("[%s] %d falhas seguidas - interrompendo coleta", nomeMercado, falhasSeguidas);
                    break;
                }
            }
            Thread.sleep(delayMs); // evita sobrecarregar/ser bloqueado pelo site
        }

        cookies.clear(); // próxima execução começa com sessão nova
        if (produtos.isEmpty()) {
            // devolve o motivo em vez de "0 produtos", para facilitar o diagnóstico
            throw new IllegalStateException("nenhum produto coletado: " + ultimoErro);
        }
        LOG.infof("[%s] coleta concluída: %d produtos", nomeMercado, produtos.size());
        return new ArrayList<>(produtos.values());
    }

    @Override
    protected List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception {
        return new ArrayList<>(parseResposta(baixar(url)).produtos().values());
    }

    private String urlPagina(int pagina) {
        return baseUrl + "/api/produto/pesquisar?categoria=" + categoriaBusca + "&pagina=" + pagina + "&janela=true";
    }

    // ---------------------------------------------------------------- sessão / cookies

    /**
     * A API só responde para quem já passou pelo site (cookie de sessão emitido na primeira visita).
     * Por isso fazemos o mesmo que o navegador: abrimos a página inicial, guardamos os cookies e
     * usamos nas chamadas à API. Se a sessão expirar (401/403 ou resposta que não é JSON),
     * a sessão é renovada automaticamente e a chamada é refeita uma vez.
     */
    private final Map<String, String> cookies = new LinkedHashMap<>();

    private synchronized void iniciarSessao() throws Exception {
        cookies.clear();
        Connection.Response home = Jsoup.connect(baseUrl + "/")
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .timeout(30_000)
                .execute();
        cookies.putAll(home.cookies());
        cookies.putAll(cookiesManuais());
        LOG.infof("[%s] sessão iniciada (HTTP %d) - cookies: %s", nomeMercado, home.statusCode(), cookies.keySet());
    }

    private String baixar(String url) throws Exception {
        if (cookies.isEmpty()) iniciarSessao();

        Connection.Response resp = chamarApi(url);
        if (!respostaValida(resp)) {
            LOG.infof("[%s] API respondeu HTTP %d (%s) - renovando sessão", nomeMercado, resp.statusCode(), inicio(resp.body()));
            iniciarSessao();
            resp = chamarApi(url);
            if (!respostaValida(resp)) {
                throw new IllegalStateException("API recusou a requisição mesmo após renovar a sessão (HTTP "
                        + resp.statusCode() + ", cookies enviados: " + cookies.keySet() + ") em " + url
                        + " - início da resposta: " + inicio(resp.body()));
            }
        }
        return resp.body();
    }

    private Connection.Response chamarApi(String url) throws Exception {
        Connection.Response resp = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .header("X-Requested-With", "XMLHttpRequest")
                .referrer(baseUrl + "/")
                .cookies(cookies)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .maxBodySize(0)
                .timeout(30_000)
                .execute();
        cookies.putAll(resp.cookies()); // o servidor pode renovar o cookie a cada resposta
        return resp;
    }

    private static String inicio(String body) {
        String t = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        return t.length() > 300 ? t.substring(0, 300) + "..." : t;
    }

    static boolean respostaValida(Connection.Response resp) {
        return resp.statusCode() == 200 && resp.body().stripLeading().startsWith("{");
    }

    // cookies extras no formato "nome=valor; nome2=valor2" (só se a visita automática não bastar)
    private Map<String, String> cookiesManuais() {
        Map<String, String> extras = new LinkedHashMap<>();
        cookieManual.ifPresent(texto -> {
            for (String par : texto.split(";")) {
                int i = par.indexOf('=');
                if (i > 0) extras.put(par.substring(0, i).trim(), par.substring(i + 1).trim());
            }
        });
        return extras;
    }

    // ---------------------------------------------------------------- leitura do JSON

    record Resposta(int paginas, Map<String, ScrapedProdutoDTO> produtos) {}

    /**
     * Formato da resposta (resumido):
     * {"produtos":[{"id":"1752","nome":"Absorvente Always ... 32 Unidades","departamento":"Higiene e Perfumaria",
     *   "categoria":"Cuidados Pessoais","subCategoria":"Absorventes","marca":"Always","unidade":"UND","valor":18.15,...}],
     *  "paginas":1011,"qtdProdutos":16172}
     */
    static Resposta parseResposta(String json) {
        JsonObject raiz;
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            raiz = reader.readObject();
        }

        int paginas = raiz.get("paginas") instanceof JsonNumber n ? n.intValue() : 0;
        Map<String, ScrapedProdutoDTO> produtos = new LinkedHashMap<>();

        for (JsonValue v : raiz.getJsonArray("produtos") == null ? List.<JsonValue>of() : raiz.getJsonArray("produtos")) {
            if (!(v instanceof JsonObject p)) continue;

            String nome = texto(p, "nome");
            BigDecimal preco = menorValor(p, "valor", "valorPromocional", "valorPromocao", "precoPromocional");
            if (nome == null || preco == null) continue;
            nome = nome.replaceAll("\\s+", " ").trim();

            // o departamento é o nível mais alto (Mercearia, Bebidas, Limpeza...); se faltar, usa a categoria
            String categoria = texto(p, "departamento");
            if (categoria == null) categoria = texto(p, "categoria");

            MedidaParser.Medida medida = medida(nome, texto(p, "unidade"));

            ScrapedProdutoDTO dto = new ScrapedProdutoDTO(nome, categoria, medida.unidade(), medida.quantidade(),
                    preco.setScale(2, RoundingMode.HALF_UP));
            dto.marca = texto(p, "marca");

            String id = texto(p, "id");
            if (id == null && p.get("id") instanceof JsonNumber n) id = n.toString();
            produtos.putIfAbsent(id != null ? id : nome.toLowerCase(Locale.ROOT), dto);
        }

        return new Resposta(paginas, produtos);
    }

    /** Produtos vendidos por quilo vêm com unidade "KG" e o valor é o preço do kg; os demais têm a medida no nome. */
    static MedidaParser.Medida medida(String nome, String unidadeSite) {
        if (unidadeSite != null && unidadeSite.trim().equalsIgnoreCase("KG")) {
            return new MedidaParser.Medida(1, "kg");
        }
        return MedidaParser.parse(nome);
    }

    private static String texto(JsonObject obj, String chave) {
        JsonValue v = obj.get(chave);
        if (v instanceof JsonString s && !s.getString().isBlank()) return s.getString().trim();
        return null;
    }

    /** Menor valor positivo entre os campos informados (preço promocional, quando existir, vence o normal). */
    private static BigDecimal menorValor(JsonObject obj, String... chaves) {
        BigDecimal menor = null;
        for (String chave : chaves) {
            BigDecimal valor = null;
            JsonValue v = obj.get(chave);
            if (v instanceof JsonNumber n) {
                valor = n.bigDecimalValue();
            } else if (v instanceof JsonString s) {
                try {
                    valor = new BigDecimal(s.getString().replace(",", "."));
                } catch (NumberFormatException ignored) {
                    // campo não numérico, ignora
                }
            }
            if (valor != null && valor.signum() > 0 && (menor == null || valor.compareTo(menor) < 0)) menor = valor;
        }
        return menor;
    }
}
