package org.controlers.DTO;

import org.entity.Usuarios;

public class AuthDTO {

    public static class Registro {
        public String nome;
        public String email;
        public String senha;
    }

    public static class Login {
        public String email;
        public String senha;
    }

    // dados públicos do usuário (nunca inclui a senha)
    public static class UsuarioInfo {
        public Long id;
        public String nome;
        public String email;
        public String role;

        public UsuarioInfo(Usuarios u) {
            this.id = u.id;
            this.nome = u.nome;
            this.email = u.email;
            this.role = u.role;
        }
    }

    public static class Resposta {
        public String token;
        public UsuarioInfo usuario;

        public Resposta(String token, Usuarios u) {
            this.token = token;
            this.usuario = new UsuarioInfo(u);
        }
    }

    public static class Erro {
        public String mensagem;

        public Erro(String mensagem) {
            this.mensagem = mensagem;
        }
    }
}
