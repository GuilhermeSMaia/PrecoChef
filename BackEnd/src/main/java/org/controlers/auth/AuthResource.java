package org.controlers.auth;

import org.auth.AuthService;
import org.controlers.DTO.AuthDTO;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.entity.Usuarios;
import org.repository.UsuarioRepository;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

// cadastro e login. Quem se cadastra pelo site é sempre "user"; o admin inicial é criado na inicialização (AdminInicial)
@Path("/auth")
public class AuthResource {
    @Inject
    UsuarioRepository usuarioRepository;
    @Inject
    AuthService authService;
    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/registrar")
    @Transactional
    public Response registrar(AuthDTO.Registro dto) {
        if (dto == null) return erro(Response.Status.BAD_REQUEST, "Dados obrigatórios");

        String nome = dto.nome == null ? "" : dto.nome.trim();
        if (nome.length() < 2 || nome.length() > 100) {
            return erro(Response.Status.BAD_REQUEST, "O nome deve ter entre 2 e 100 caracteres");
        }
        if (!authService.emailValido(dto.email)) return erro(Response.Status.BAD_REQUEST, "E-mail inválido");
        String problemaSenha = authService.validarSenha(dto.senha);
        if (problemaSenha != null) return erro(Response.Status.BAD_REQUEST, problemaSenha);

        String email = dto.email.trim().toLowerCase();
        if (usuarioRepository.findByEmail(email) != null) {
            return erro(Response.Status.CONFLICT, "Já existe uma conta com este e-mail");
        }

        Usuarios usuario = new Usuarios(nome, email, authService.hash(dto.senha), Usuarios.ROLE_USER);
        usuarioRepository.persist(usuario);

        return Response.status(Response.Status.CREATED)
                .entity(new AuthDTO.Resposta(authService.gerarToken(usuario), usuario))
                .build();
    }

    @POST
    @Path("/login")
    public Response login(AuthDTO.Login dto) {
        if (dto == null) return erro(Response.Status.BAD_REQUEST, "Dados obrigatórios");
        Usuarios usuario = usuarioRepository.findByEmail(dto.email);
        // mesma mensagem (e mesmo tempo) para e-mail inexistente e senha errada: não revela quais e-mails têm conta
        if (!authService.senhaConfere(dto.senha, usuario == null ? null : usuario.senhaHash)) {
            return erro(Response.Status.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
        return Response.ok(new AuthDTO.Resposta(authService.gerarToken(usuario), usuario)).build();
    }

    // confere se o token ainda vale e devolve os dados atuais do usuário (ex.: role alterada)
    @GET
    @Path("/me")
    @Authenticated
    public Response me() {
        Usuarios usuario = usuarioRepository.findById(Long.valueOf(jwt.getSubject()));
        if (usuario == null) return erro(Response.Status.UNAUTHORIZED, "Usuário não encontrado");
        return Response.ok(new AuthDTO.UsuarioInfo(usuario)).build();
    }

    private Response erro(Response.Status status, String mensagem) {
        return Response.status(status).entity(new AuthDTO.Erro(mensagem)).build();
    }
}
