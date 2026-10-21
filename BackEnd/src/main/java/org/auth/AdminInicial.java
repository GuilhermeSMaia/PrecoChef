package org.auth;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.entity.Usuarios;
import org.jboss.logging.Logger;
import org.repository.UsuarioRepository;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

// pelo site só se cadastram contas "user"; o primeiro admin vem da configuração (ADMIN_EMAIL / ADMIN_SENHA).
// Só age enquanto não existir nenhum admin: depois disso, mudar as variáveis não altera nada
@ApplicationScoped
public class AdminInicial {
    private static final Logger LOG = Logger.getLogger(AdminInicial.class);

    @Inject
    UsuarioRepository usuarioRepository;
    @Inject
    AuthService authService;

    @ConfigProperty(name = "admin.email")
    Optional<String> email;
    @ConfigProperty(name = "admin.senha")
    Optional<String> senha;
    @ConfigProperty(name = "admin.nome", defaultValue = "Administrador")
    String nome;

    @Transactional
    void criar(@Observes StartupEvent evento) {
        if (usuarioRepository.existeAdmin()) return;

        if (email.filter(e -> !e.isBlank()).isEmpty() || senha.filter(s -> !s.isBlank()).isEmpty()) {
            LOG.warn("Nenhum admin cadastrado. Defina ADMIN_EMAIL e ADMIN_SENHA para criar o primeiro.");
            return;
        }
        if (!authService.emailValido(email.get())) {
            LOG.errorf("ADMIN_EMAIL inválido: %s", email.get());
            return;
        }
        String problemaSenha = authService.validarSenha(senha.get());
        if (problemaSenha != null) {
            LOG.errorf("ADMIN_SENHA não atende às regras: %s", problemaSenha);
            return;
        }

        // se o e-mail já tem conta "user", ela é promovida (a senha dela é mantida)
        Usuarios existente = usuarioRepository.findByEmail(email.get());
        if (existente != null) {
            existente.role = Usuarios.ROLE_ADMIN;
            LOG.infof("Usuário %s promovido a admin", existente.email);
            return;
        }

        Usuarios admin = new Usuarios(nome, email.get().trim().toLowerCase(), authService.hash(senha.get()), Usuarios.ROLE_ADMIN);
        usuarioRepository.persist(admin);
        LOG.infof("Admin inicial criado: %s", admin.email);
    }
}
