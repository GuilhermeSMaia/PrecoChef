package org.scraper;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Extrai quantidade + unidade a partir do nome do produto.
// Ex.: "Arroz Tio João Tipo 1 5kg" -> 5 kg | "Refrigerante Coca-Cola 2L" -> 2 l | "Banana Prata Kg" -> 1 kg
public final class MedidaParser {

    public static final String PADRAO = "un";

    private static final Pattern QTD_UNIDADE = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kg|kgs|quilos?|gr|grs|gramas?|g|mg|ml|litros?|lts?|l|un|und|unds|unid|unidades?)(?![a-zà-ú])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // produtos vendidos a granel: "Tomate Kg", "Carne Moída Kg"
    private static final Pattern GRANEL = Pattern.compile("(?:^|\\s|/)(kg)(?![a-zà-ú])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private MedidaParser() {}

    public record Medida(float quantidade, String unidade) {}

    public static Medida parse(String nome) {
        if (nome == null || nome.isBlank()) return new Medida(1, PADRAO);

        // usa a ÚLTIMA ocorrência: em "Cerveja 12x350ml" queremos "350 ml"
        Matcher m = QTD_UNIDADE.matcher(nome);
        Medida ultima = null;
        while (m.find()) {
            try {
                float qtd = Float.parseFloat(m.group(1).replace(',', '.'));
                ultima = new Medida(qtd, normalizarUnidade(m.group(2)));
            } catch (NumberFormatException ignored) {
                // segue para a próxima ocorrência
            }
        }
        if (ultima != null) return ultima;

        if (GRANEL.matcher(nome).find()) return new Medida(1, "kg");

        return new Medida(1, PADRAO);
    }

    public static String normalizarUnidade(String unidade) {
        String u = unidade.toLowerCase(Locale.ROOT);
        if (u.startsWith("kg") || u.startsWith("quilo")) return "kg";
        if (u.equals("mg")) return "mg";
        if (u.equals("ml")) return "ml";
        if (u.startsWith("g")) return "g";
        if (u.startsWith("l")) return "l";
        return PADRAO;
    }
}
