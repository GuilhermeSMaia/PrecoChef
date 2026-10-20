package org.controlers.mercados;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.controlers.DTO.CreateMercadoDTO;
import org.controlers.DTO.EnderecoDTO;
import org.entity.Enderecos;
import org.entity.Mercados;
import org.repository.MercadoRepository;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/mercados")
public class CreateMercados {
    @Inject
    MercadoRepository mercadoRepository;

    @Path("/create")
    @POST
    @Transactional
    public Response createMercados(CreateMercadoDTO dto) {
        List<EnderecoDTO> enderecos = dto.enderecos == null ? List.of() : dto.enderecos.stream()
                .filter(e -> e != null && e.endereco != null && !e.endereco.isBlank())
                .toList();

        if (dto.mercadoId == null) {
            Mercados mercadoExistente = mercadoRepository.findByName(dto.name);
            if (mercadoExistente != null) {
                return Response.status(Response.Status.CONFLICT).entity("Mercado já cadastrado").build();
            }

            Mercados mercado = new Mercados(dto.name);
            mercado.setUrl(normalizarUrl(dto.url));
            enderecos.forEach(e -> mercado.adicionarEndereco(e.descricao, e.endereco.trim()));
            mercadoRepository.persist(mercado);
            return Response.ok("Mercado criado com sucesso").build();
        }

        Mercados mercado = mercadoRepository.findById(dto.mercadoId);
        if (mercado == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Mercado não encontrado").build();
        }
        mercado.setNome(dto.name);
        mercado.setUrl(normalizarUrl(dto.url));
        atualizarEnderecos(mercado, enderecos);
        mercado.persist();
        return Response.ok("Mercado atualizado com sucesso").build();
    }

    // vazio vira null; sem protocolo ganha https:// (ex.: "www.irmaosgoncalves.com.br")
    private String normalizarUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String u = url.trim();
        return u.matches("(?i)^https?://.*") ? u : "https://" + u;
    }

    // a lista enviada é a lista completa: com id = edita, sem id = adiciona, ausente da lista = remove
    private void atualizarEnderecos(Mercados mercado, List<EnderecoDTO> enderecos) {
        Map<Long, Enderecos> atuais = mercado.getEnderecos().stream()
                .collect(Collectors.toMap(Enderecos::getId, Function.identity()));

        List<Enderecos> mantidos = new ArrayList<>();
        for (EnderecoDTO e : enderecos) {
            Enderecos existente = e.id != null ? atuais.get(e.id) : null;
            if (existente != null) {
                existente.setDescricao(e.descricao);
                existente.setEndereco(e.endereco.trim());
                mantidos.add(existente);
            } else {
                mantidos.add(new Enderecos(e.descricao, e.endereco.trim(), mercado));
            }
        }

        mercado.getEnderecos().clear();
        mercado.getEnderecos().addAll(mantidos); // os que ficaram de fora são apagados (orphanRemoval)
    }
}
