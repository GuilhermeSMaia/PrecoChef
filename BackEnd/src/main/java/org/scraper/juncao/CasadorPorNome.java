package org.scraper.juncao;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liga um item sem código de barras a um produto já cadastrado por outro mercado, comparando nomes
 * ("Cerveja Pilsen Skol Lata 269ml" = "Cerveja Skol Pilsen 269ml").
 *
 * As regras são rígidas de propósito (prefere não ligar a ligar errado):
 *  - mesmo tamanho (269ml, 1,15kg = 1150g...) e mesmo tipo de embalagem (avulso x pack "c/15", "21x8g");
 *  - marca compatível (a marca de um aparece no outro);
 *  - todas as palavras do nome mais curto aparecem no mais longo: assim "Difusor Coala Patchouli" não
 *    casa com "Difusor Coala Lavanda" (cada um tem uma palavra que o outro não tem);
 *  - a palavra a mais do nome longo não pode indicar outra versão (sabor, light, zero, sem, kids...):
 *    "Molho Shoyu Light Mitsuwa" não casa com "Molho Shoyu Mitsuwa";
 *  - um único candidato vencedor; empate = não liga.
 */
public class CasadorPorNome<T> {

    private static final Pattern QTD = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(kg|g|gr|grs|mg|l|lt|lts|litro|litros|ml)\\b");
    private static final Pattern PACK_X = Pattern.compile("\\b(\\d+)\\s*x\\s*\\d");
    private static final Pattern PACK_COM = Pattern.compile("\\bc/\\s*(\\d+)");
    private static final Pattern PACK_UN = Pattern.compile("\\b(\\d+)\\s*(?:un|und|uni|unid|unids|unidades)\\b");
    private static final Set<String> PACK_PALAVRAS = Set.of("kit", "leve", "pague", "fardo", "multipack");

    // palavras de embalagem/ligação que não identificam o produto
    private static final Set<String> PARADAS = Set.of(
            "de", "da", "do", "das", "dos", "e", "com", "em", "para", "a", "o", "as", "os",
            "un", "und", "uni", "unid", "unidade", "unidades", "pacote", "pct", "pc", "emb", "embalagem",
            "frasco", "fr", "lata", "vidro", "pote", "garrafa", "gf", "pet", "sache", "sachet", "caixa", "cx",
            "bandeja", "refil", "tp", "tetra", "pak", "bisnaga", "squeeze", "plastico", "plast", "blister",
            "one", "way", "long", "neck");

    // palavras que mudam a versão do produto: se aparecem em só um dos nomes, são produtos diferentes
    private static final Set<String> VARIANTES = Set.of(
            "zero", "light", "diet", "fit", "sem", "lactose", "integral", "desnatado", "semidesnatado",
            "kids", "kid", "infantil", "junior", "teen", "shot", "mousse", "cookies", "cookie", "amargo", "intenso",
            "frutas", "vermelhas", "verm", "limao", "laranja", "morango", "uva", "maracuja", "abacaxi", "coco",
            "baunilha", "chocolate", "branco", "preto", "amendoim", "avela", "pistache", "caramelo", "mel", "canela",
            "melancia", "manga", "goiaba", "caju", "tangerina", "pessego", "cereja", "framboesa", "banana", "maca",
            "menta", "hortela", "menthol", "lavanda", "camomila", "calendula", "rosa", "rose", "tinto",
            "picante", "pimenta", "apimentado", "defumado", "cebola", "alho", "queijo", "bacon", "frango", "carne",
            "tomate", "natural", "oleo", "agua", "sal", "acucar", "cafe", "leite", "levinho",
            "cozido", "cru", "descascado", "temperado", "empanado");

    // abreviações comuns nos nomes dos sites
    private static final Map<String, String> SINONIMOS = Map.of(
            "rech", "recheado", "recheio", "recheado", "choc", "chocolate", "refri", "refrigerante",
            "trad", "tradicional", "integ", "integral", "desn", "desnatado");

    /** Características do nome usadas na comparação. */
    public record Assinatura(String tamanho, int pack, Set<String> palavras, Set<String> marca) {
        boolean comparavel() {
            return tamanho != null && palavras.size() >= 2;
        }
    }

    private record Candidato<T>(T valor, Assinatura assinatura) {}

    // índice por tamanho + embalagem: só compara com quem tem a mesma medida
    private final Map<String, List<Candidato<T>>> porTamanho = new HashMap<>();
    private final Map<T, String> chaveDoValor = new HashMap<>();

    public void adicionar(T valor, String nome, String marca) {
        Assinatura a = assinatura(nome, marca);
        if (!a.comparavel()) return;
        String chave = a.tamanho() + "#" + a.pack();
        porTamanho.computeIfAbsent(chave, k -> new ArrayList<>()).add(new Candidato<>(valor, a));
        chaveDoValor.put(valor, chave);
    }

    /** Tira um candidato (ex.: já foi ligado a outro item deste mercado). */
    public void remover(T valor) {
        String chave = chaveDoValor.remove(valor);
        if (chave != null) porTamanho.get(chave).removeIf(c -> c.valor().equals(valor));
    }

    /** O único candidato que é o mesmo produto, ou null se nenhum (ou mais de um empatado). */
    public T encontrar(String nome, String marca) {
        Assinatura item = assinatura(nome, marca);
        if (!item.comparavel()) return null;

        T melhor = null;
        double melhorNota = -1;
        boolean empate = false;
        for (Candidato<T> c : porTamanho.getOrDefault(item.tamanho() + "#" + item.pack(), List.of())) {
            double nota = nota(item, c.assinatura());
            if (nota < 0) continue;
            if (nota > melhorNota) {
                melhor = c.valor();
                melhorNota = nota;
                empate = false;
            } else if (nota == melhorNota) {
                empate = true;
            }
        }
        return empate ? null : melhor;
    }

    /** Semelhança (jaccard das palavras) quando as regras aceitam o par; -1 quando não é o mesmo produto. */
    static double nota(Assinatura a, Assinatura b) {
        if (!a.comparavel() || !b.comparavel()) return -1;
        if (!a.tamanho().equals(b.tamanho()) || a.pack() != b.pack()) return -1;

        // marca: a de um precisa aparecer no outro; sem marca nos dois é arriscado demais
        Set<String> todasB = uniao(b.palavras(), b.marca());
        Set<String> todasA = uniao(a.palavras(), a.marca());
        if (!a.marca().isEmpty()) {
            if (!todasB.containsAll(a.marca())) return -1;
        } else if (!b.marca().isEmpty()) {
            if (!todasA.containsAll(b.marca())) return -1;
        } else {
            return -1;
        }

        Set<String> menor = a.palavras().size() <= b.palavras().size() ? a.palavras() : b.palavras();
        Set<String> maior = menor == a.palavras() ? b.palavras() : a.palavras();
        if (!maior.containsAll(menor)) return -1;

        // o que só o nome mais longo tem não pode ser versão (sabor, light...) nem código ("3d", "2em1")
        for (String extra : maior) {
            if (menor.contains(extra)) continue;
            if (VARIANTES.contains(extra) || extra.chars().anyMatch(Character::isDigit)) return -1;
        }

        double jaccard = (double) menor.size() / uniao(a.palavras(), b.palavras()).size();
        return jaccard >= 0.5 ? jaccard : -1;
    }

    public static Assinatura assinatura(String nome, String marca) {
        String n = normalizar(nome);
        return new Assinatura(tamanho(n), pack(n), palavras(n), palavras(normalizar(marca)));
    }

    static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    /** "900ml" -> "ml:900"; "1,15kg" e "1.150Kg" -> "g:1150". Usa a última medida do nome. */
    static String tamanho(String n) {
        Matcher m = QTD.matcher(n);
        String num = null, un = null;
        while (m.find()) {
            num = m.group(1);
            un = m.group(2);
        }
        if (num == null) return null;
        double v = Double.parseDouble(num.replace(',', '.'));
        String base;
        switch (un) {
            case "kg" -> { v *= 1000; base = "g"; }
            case "g", "gr", "grs" -> base = "g";
            case "mg" -> { v /= 1000; base = "g"; }
            case "ml" -> base = "ml";
            default -> { v *= 1000; base = "ml"; } // l, lt, litro...
        }
        return base + ":" + Math.round(v * 100) / 100.0;
    }

    /** 1 = avulso; n = pack com n unidades; -1 = kit/combo/leve-pague (nunca casa com avulso). */
    static int pack(String n) {
        if (n.contains(" + ")) return -1; // combo: "Cereal Nescau 210g + Snow Flakes 230g"
        for (Pattern p : List.of(PACK_X, PACK_COM, PACK_UN)) {
            Matcher m = p.matcher(n);
            if (m.find()) {
                int q = Integer.parseInt(m.group(1));
                if (q > 1) return q;
            }
        }
        for (String palavra : n.split("[^a-z]+")) {
            if (PACK_PALAVRAS.contains(palavra)) return -1;
        }
        return 1;
    }

    static Set<String> palavras(String n) {
        String limpo = QTD.matcher(n).replaceAll(" ").replaceAll("[^a-z0-9 ]", " ");
        Set<String> palavras = new HashSet<>();
        for (String p : limpo.split("\\s+")) {
            if (p.length() <= 1 || p.chars().allMatch(Character::isDigit)) continue;
            p = SINONIMOS.getOrDefault(p, p);
            if (!PARADAS.contains(p)) palavras.add(p);
        }
        return palavras;
    }

    private static Set<String> uniao(Set<String> a, Set<String> b) {
        Set<String> u = new HashSet<>(a);
        u.addAll(b);
        return u;
    }
}
