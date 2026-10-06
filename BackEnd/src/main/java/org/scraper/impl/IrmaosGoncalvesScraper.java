package org.scraper.impl;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
 * Scraper do site https://www.irmaosgoncalves.com.br
 *
 * O site organiza os produtos em departamentos com URLs do tipo
 * /categoria/{departamento} e /categoria/{departamento}/{subcategoria}
 * (ex.: /categoria/mercearia, /categoria/bebidas/refrigerantes).
 *
 * Cada departamento vira uma Categoria no banco (ex.: "Mercearia", "Bebidas"),
 * e todos os produtos encontrados nele recebem essa categoria.
 *
 * Os produtos são extraídos, nesta ordem de preferência:
 *  1. JSON-LD (schema.org Product / ItemList), quando o site publica
 *  2. JSON do Next.js (__NEXT_DATA__), quando o site é renderizado com Next
 *  3. HTML dos cards de produto (seletores CSS configuráveis no application.properties)
 */
@ApplicationScoped
public class IrmaosGoncalvesScraper extends AbstractMercadoScraper {

    private static final Logger LOG = Logger.getLogger(IrmaosGoncalvesScraper.class);
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Safari/537.36";
    static final String SELETOR_CARD_PADRAO =
            "[class*=product-card], [class*=ProductCard], [class*=product-item], [class*=productCard], [class*=card-produto], [data-product-id], [data-sku]";
    static final String SELETOR_NOME_PADRAO =
            "[class*=name], [class*=Name], [class*=nome], [class*=title], [class*=Title], h2, h3";
    private static final Pattern PRECO = Pattern.compile("R\\$\\s*(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+,\\d{2})");

    // departamentos conhecidos do site (slug -> nome da categoria). Usados se a descoberta automática pelo menu falhar.
    static final Map<String, String> DEPARTAMENTOS_CONHECIDOS = new LinkedHashMap<>();
    static {
        DEPARTAMENTOS_CONHECIDOS.put("mercearia", "Mercearia");
        DEPARTAMENTOS_CONHECIDOS.put("bebidas", "Bebidas");
        DEPARTAMENTOS_CONHECIDOS.put("hortifruti", "Hortifruti");
        DEPARTAMENTOS_CONHECIDOS.put("acougue", "Açougue");
        DEPARTAMENTOS_CONHECIDOS.put("padaria", "Padaria");
        DEPARTAMENTOS_CONHECIDOS.put("frios-e-laticinios", "Frios e Laticínios");
        DEPARTAMENTOS_CONHECIDOS.put("congelados--resfriados-e-sobremesas", "Congelados, Resfriados e Sobremesas");
        DEPARTAMENTOS_CONHECIDOS.put("limpeza", "Limpeza");
        DEPARTAMENTOS_CONHECIDOS.put("higiene-e-cuidados-pessoais", "Higiene e Cuidados Pessoais");
        DEPARTAMENTOS_CONHECIDOS.put("utilidades-e-casa", "Utilidades e Casa");
        DEPARTAMENTOS_CONHECIDOS.put("pet-shop", "Pet Shop");
        DEPARTAMENTOS_CONHECIDOS.put("bebes-e-infantil", "Bebês e Infantil");
    }

    @ConfigProperty(name = "scraping.irmaosgoncalves.nome-mercado", defaultValue = "Irmãos Gonçalves")
    String nomeMercado;

    @ConfigProperty(name = "scraping.irmaosgoncalves.base-url", defaultValue = "https://www.irmaosgoncalves.com.br")
    String baseUrl;

    // parâmetro de paginação da listagem (ex.: ?page=2). O scraper para quando uma página não traz produto novo.
    @ConfigProperty(name = "scraping.irmaosgoncalves.param-pagina", defaultValue = "page")
    String paramPagina;

    @ConfigProperty(name = "scraping.irmaosgoncalves.max-paginas", defaultValue = "30")
    int maxPaginas;

    // seletores do fallback em HTML - ajuste aqui caso o layout do site mude
    @ConfigProperty(name = "scraping.irmaosgoncalves.seletor-card", defaultValue = SELETOR_CARD_PADRAO)
    String seletorCard;

    @ConfigProperty(name = "scraping.irmaosgoncalves.seletor-nome", defaultValue = SELETOR_NOME_PADRAO)
    String seletorNome;

    // departamento descoberto: url -> nome da categoria
    private final Map<String, String> categoriasPorUrl = new LinkedHashMap<>();

    @Override
    public String getNomeMercado() {
        return nomeMercado;
    }

    @Override
    public List<String> getUrls() {
        categoriasPorUrl.clear();
        try {
            categoriasPorUrl.putAll(descobrirDepartamentos(baixar(baseUrl), baseUrl));
        } catch (Exception e) {
            LOG.warnf(e, "[%s] não foi possível ler o menu de departamentos - usando lista padrão", nomeMercado);
        }

        if (categoriasPorUrl.isEmpty()) {
            DEPARTAMENTOS_CONHECIDOS.forEach((slug, nome) -> categoriasPorUrl.put(baseUrl + "/categoria/" + slug, nome));
        }

        LOG.infof("[%s] %d departamentos: %s", nomeMercado, categoriasPorUrl.size(), categoriasPorUrl.values());
        return new ArrayList<>(categoriasPorUrl.keySet());
    }

    @Override
    protected List<ScrapedProdutoDTO> scrapePagina(String url) throws Exception {
        String categoria = categoriasPorUrl.getOrDefault(url, nomeDoSlug(slugDepartamento(url)));

        Map<String, ScrapedProdutoDTO> produtos = new LinkedHashMap<>(); // chave = nome, evita duplicados entre páginas

        for (int pagina = 1; pagina <= maxPaginas; pagina++) {
            String urlPagina = pagina == 1 ? url : url + (url.contains("?") ? "&" : "?") + paramPagina + "=" + pagina;
            Document doc = baixar(urlPagina);

            int antes = produtos.size();
            for (ScrapedProdutoDTO p : extrairProdutos(doc, categoria, seletorCard, seletorNome)) {
                produtos.putIfAbsent(p.nome.toLowerCase(Locale.ROOT), p);
            }

            // página sem produto novo = fim da listagem (ou o site ignora o parâmetro de paginação)
            if (produtos.size() == antes) break;
            if (pagina < maxPaginas) Thread.sleep(1000);
        }

        return new ArrayList<>(produtos.values());
    }

    private Document baixar(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .timeout(20_000)
                .get();
    }

    // ---------------------------------------------------------------- departamentos

    /** Lê os links "/categoria/{slug}" do menu e devolve apenas os departamentos de primeiro nível. */
    static Map<String, String> descobrirDepartamentos(Document home, String baseUrl) {
        Map<String, String> departamentos = new LinkedHashMap<>();

        for (Element link : home.select("a[href*=/categoria/]")) {
            String slug = slugDepartamento(link.attr("href"));
            if (slug == null) continue;

            String url = baseUrl + "/categoria/" + slug;
            String nomeLink = link.text().trim();

            // links de subcategoria apontam para o mesmo departamento; o nome vem do link do próprio departamento
            String href = URLDecoder.decode(link.attr("href"), StandardCharsets.UTF_8)
                    .replaceAll("[?#].*$", "").replaceAll("/+$", "").toLowerCase(Locale.ROOT);
            boolean linkDoDepartamento = href.endsWith("/categoria/" + slug);

            if (linkDoDepartamento && !nomeLink.isBlank() && nomeLink.length() <= 60) {
                departamentos.put(url, nomeLink);
            } else {
                departamentos.putIfAbsent(url, DEPARTAMENTOS_CONHECIDOS.getOrDefault(slug, nomeDoSlug(slug)));
            }
        }
        return departamentos;
    }

    /** "https://.../categoria/bebidas/refrigerantes?x=1" -> "bebidas" */
    static String slugDepartamento(String url) {
        if (url == null) return null;
        int i = url.indexOf("/categoria/");
        if (i < 0) return null;
        String resto = url.substring(i + "/categoria/".length()).replaceAll("[?#].*$", "");
        String slug = resto.split("/")[0];
        if (slug.isBlank()) return null;
        return URLDecoder.decode(slug, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    }

    /** "congelados--resfriados-e-sobremesas" -> "Congelados, Resfriados e Sobremesas" */
    static String nomeDoSlug(String slug) {
        if (slug == null || slug.isBlank()) return "Outros";
        if (DEPARTAMENTOS_CONHECIDOS.containsKey(slug)) return DEPARTAMENTOS_CONHECIDOS.get(slug);

        String[] palavras = slug.replace("--", ", ").replace('-', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : palavras) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            boolean conectivo = Set.of("e", "de", "da", "do", "das", "dos", "para", "com").contains(p);
            sb.append(conectivo && sb.length() > 0 ? p : Character.toUpperCase(p.charAt(0)) + p.substring(1));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- produtos

    static List<ScrapedProdutoDTO> extrairProdutos(Document doc, String categoria, String seletorCard, String seletorNome) {
        List<ScrapedProdutoDTO> produtos = extrairDeJsonLd(doc, categoria);
        if (produtos.isEmpty()) produtos = extrairDeNextData(doc, categoria);
        if (produtos.isEmpty()) produtos = extrairDeHtml(doc, categoria, seletorCard, seletorNome);
        return produtos;
    }

    static List<ScrapedProdutoDTO> extrairDeJsonLd(Document doc, String categoria) {
        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        for (Element script : doc.select("script[type=application/ld+json]")) {
            JsonValue json = lerJson(script.data());
            if (json != null) coletarProdutosSchemaOrg(json, categoria, produtos);
        }
        return produtos;
    }

    private static void coletarProdutosSchemaOrg(JsonValue json, String categoria, List<ScrapedProdutoDTO> produtos) {
        if (json instanceof JsonArray arr) {
            arr.forEach(v -> coletarProdutosSchemaOrg(v, categoria, produtos));
            return;
        }
        if (!(json instanceof JsonObject obj)) return;

        String tipo = obj.get("@type") instanceof JsonString s ? s.getString() : "";
        if ("Product".equalsIgnoreCase(tipo)) {
            String nome = texto(obj, "name");
            BigDecimal preco = null;
            JsonValue offers = obj.get("offers");
            if (offers instanceof JsonArray a && !a.isEmpty()) offers = a.get(0);
            if (offers instanceof JsonObject o) {
                preco = numero(o, "price", "lowPrice");
            }
            adicionar(produtos, nome, preco, categoria);
            return;
        }

        // ItemList, @graph, item, etc.
        for (String chave : List.of("itemListElement", "@graph", "item")) {
            if (obj.containsKey(chave)) coletarProdutosSchemaOrg(obj.get(chave), categoria, produtos);
        }
    }

    static List<ScrapedProdutoDTO> extrairDeNextData(Document doc, String categoria) {
        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        Element script = doc.selectFirst("script#__NEXT_DATA__");
        if (script == null) return produtos;

        JsonValue json = lerJson(script.data());
        if (json != null) coletarProdutosGenerico(json, categoria, produtos, 0);
        return produtos;
    }

    private static final List<String> CHAVES_NOME = List.of("name", "nome", "descricao", "description", "title", "productName");
    private static final List<String> CHAVES_PRECO = List.of(
            "precoPromocional", "preco_promocional", "promotionalPrice", "salePrice", "bestPrice", "precoPor",
            "price", "preco", "precoVenda", "preco_venda", "valor", "sellingPrice", "listPrice");

    /** Percorre o JSON procurando objetos que tenham um campo de nome e um de preço. */
    private static void coletarProdutosGenerico(JsonValue json, String categoria, List<ScrapedProdutoDTO> produtos, int profundidade) {
        if (profundidade > 25) return;

        if (json instanceof JsonArray arr) {
            arr.forEach(v -> coletarProdutosGenerico(v, categoria, produtos, profundidade + 1));
            return;
        }
        if (!(json instanceof JsonObject obj)) return;

        String nome = null;
        for (String k : CHAVES_NOME) {
            nome = texto(obj, k);
            if (nome != null) break;
        }
        BigDecimal preco = numero(obj, CHAVES_PRECO.toArray(String[]::new));

        if (nome != null && preco != null) {
            adicionar(produtos, nome, preco, categoria);
            return;
        }

        obj.values().forEach(v -> coletarProdutosGenerico(v, categoria, produtos, profundidade + 1));
    }

    static List<ScrapedProdutoDTO> extrairDeHtml(Document doc, String categoria, String seletorCard, String seletorNome) {
        String cardSel = seletorCard != null ? seletorCard : SELETOR_CARD_PADRAO;
        String nomeSel = seletorNome != null ? seletorNome : SELETOR_NOME_PADRAO;

        List<ScrapedProdutoDTO> produtos = new ArrayList<>();
        Set<Element> vistos = new LinkedHashSet<>();

        for (Element card : doc.select(cardSel)) {
            // ignora cards aninhados (ex.: wrapper .product-card > .product-card__info)
            if (card.parents().stream().anyMatch(vistos::contains)) continue;

            BigDecimal preco = menorPreco(card.text());
            if (preco == null) continue;

            Element elNome = card.selectFirst(nomeSel);
            String nome = elNome != null ? elNome.text() : card.attr("title");
            if (nome.isBlank()) {
                Element img = card.selectFirst("img[alt]");
                if (img != null) nome = img.attr("alt");
            }

            vistos.add(card);
            adicionar(produtos, nome, preco, categoria);
        }
        return produtos;
    }

    // ---------------------------------------------------------------- utilitários

    private static void adicionar(List<ScrapedProdutoDTO> produtos, String nome, BigDecimal preco, String categoria) {
        if (nome == null || preco == null || preco.signum() <= 0) return;
        nome = nome.replaceAll("\\s+", " ").trim();
        if (nome.isEmpty()) return;

        MedidaParser.Medida medida = MedidaParser.parse(nome);
        produtos.add(new ScrapedProdutoDTO(nome, categoria, medida.unidade(), medida.quantidade(),
                preco.setScale(2, RoundingMode.HALF_UP)));
    }

    /** Em cards com "De R$ 10,99 Por R$ 8,99" ficamos com o menor valor (preço promocional). */
    static BigDecimal menorPreco(String texto) {
        Matcher m = PRECO.matcher(texto);
        BigDecimal menor = null;
        while (m.find()) {
            BigDecimal valor = new BigDecimal(m.group(1).replace(".", "").replace(",", "."));
            if (valor.signum() > 0 && (menor == null || valor.compareTo(menor) < 0)) menor = valor;
        }
        return menor;
    }

    private static JsonValue lerJson(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try (JsonReader reader = Json.createReader(new StringReader(texto.trim()))) {
            return reader.readValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static String texto(JsonObject obj, String chave) {
        JsonValue v = obj.get(chave);
        if (v instanceof JsonString s && !s.getString().isBlank()) return s.getString();
        return null;
    }

    private static BigDecimal numero(JsonObject obj, String... chaves) {
        for (String chave : chaves) {
            JsonValue v = obj.get(chave);
            if (v instanceof JsonNumber n && n.bigDecimalValue().signum() > 0) return n.bigDecimalValue();
            if (v instanceof JsonString s) {
                String t = s.getString().replaceAll("[^0-9,.]", "");
                if (t.isEmpty()) continue;
                // "8,99" (pt-BR) ou "8.99"
                if (t.contains(",")) t = t.replace(".", "").replace(",", ".");
                try {
                    BigDecimal b = new BigDecimal(t);
                    if (b.signum() > 0) return b;
                } catch (NumberFormatException ignored) {
                    // tenta a próxima chave
                }
            }
        }
        return null;
    }
}
