package org.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.entity.Usuarios;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthService {
    public static final String ISSUER = "precochef";

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern TEM_LETRA = Pattern.compile("\\p{L}");
    private static final Pattern TEM_NUMERO = Pattern.compile("\\d");

    // comparado quando o e-mail não existe, para o login levar o mesmo tempo nos dois casos
    // (sem isso, a resposta mais rápida revelaria quais e-mails têm conta)
    private static final String HASH_FALSO = BcryptUtil.bcryptHash("hash-falso-para-tempo-constante");

    @ConfigProperty(name = "jwt.duracao-horas", defaultValue = "8")
    long duracaoHoras;

    // regras obrigatórias (as recomendações extras ficam no front)
    public String validarSenha(String senha) {
        if (senha == null || senha.length() < 8) return "A senha deve ter pelo menos 8 caracteres";
        // o bcrypt só considera os primeiros 72 bytes
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) return "A senha deve ter no máximo 72 caracteres";
        if (!TEM_LETRA.matcher(senha).find()) return "A senha deve ter pelo menos uma letra";
        if (!TEM_NUMERO.matcher(senha).find()) return "A senha deve ter pelo menos um número";
        return null;
    }

    public boolean emailValido(String email) {
        return email != null && email.length() <= 254 && EMAIL.matcher(email.trim()).matches();
    }

    public String hash(String senha) {
        return BcryptUtil.bcryptHash(senha);
    }

    public boolean senhaConfere(String senha, String hash) {
        if (senha == null) senha = "";
        if (hash == null) {
            BcryptUtil.matches(senha, HASH_FALSO);
            return false;
        }
        return BcryptUtil.matches(senha, hash);
    }

    public String gerarToken(Usuarios usuario) {
        return Jwt.issuer(ISSUER)
                .subject(String.valueOf(usuario.id))
                .upn(usuario.email)
                .groups(Set.of(usuario.role))
                .claim("nome", usuario.nome)
                .expiresIn(Duration.ofHours(duracaoHoras))
                .sign(); // chave e algoritmo vêm de smallrye.jwt.sign.key / new-token.signature-algorithm
    }
}
