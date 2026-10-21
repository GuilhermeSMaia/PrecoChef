package org.controlers.lista;

import java.io.StringReader;
import java.time.LocalDateTime;

import org.controlers.DTO.AuthDTO;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.entity.ListasCompras;
import org.entity.Usuarios;
import org.repository.UsuarioRepository;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

// lista de compras mais recente do usuário logado. Sem login a lista fica só no navegador
@Path("/lista")
@RolesAllowed({ Usuarios.ROLE_USER, Usuarios.ROLE_ADMIN })
public class ListaComprasResource {
    private static final int MAX_ITENS = 500;

    @Inject
    UsuarioRepository usuarioRepository;
    @Inject
    JsonWebToken jwt;

    public static class ListaDTO {
        public String nome;
        public JsonArray itens;
        public LocalDateTime atualizadaEm;
    }

    @GET
    public Response obter() {
        Usuarios usuario = usuarioLogado();
        if (usuario == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        ListasCompras lista = usuarioRepository.findLista(usuario);
        if (lista == null) return Response.noContent().build();

        ListaDTO dto = new ListaDTO();
        dto.nome = lista.nome;
        try (JsonReader reader = Json.createReader(new StringReader(lista.itens))) {
            dto.itens = reader.readArray();
        }
        dto.atualizadaEm = lista.atualizadaEm;
        return Response.ok(dto).build();
    }

    // substitui a lista salva pela enviada
    @PUT
    @Transactional
    public Response salvar(ListaDTO dto) {
        Usuarios usuario = usuarioLogado();
        if (usuario == null) return Response.status(Response.Status.UNAUTHORIZED).build();
        if (dto == null || dto.itens == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new AuthDTO.Erro("Itens obrigatórios")).build();
        }
        if (dto.itens.size() > MAX_ITENS) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new AuthDTO.Erro("A lista pode ter no máximo " + MAX_ITENS + " itens")).build();
        }

        ListasCompras lista = usuarioRepository.findLista(usuario);
        if (lista == null) {
            lista = new ListasCompras();
            lista.usuario = usuario;
        }
        String nome = dto.nome == null ? "" : dto.nome.trim();
        lista.nome = nome.isEmpty() ? "Minha Lista de Compras" : nome.substring(0, Math.min(nome.length(), 100));
        lista.itens = dto.itens.toString();
        lista.atualizadaEm = LocalDateTime.now();
        lista.persist();

        dto.nome = lista.nome;
        dto.atualizadaEm = lista.atualizadaEm;
        return Response.ok(dto).build();
    }

    private Usuarios usuarioLogado() {
        return usuarioRepository.findById(Long.valueOf(jwt.getSubject()));
    }
}
