package org.controlers.mercados;

import org.controlers.DTO.CreateMercadoDTO;
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
        if (dto.mercadoId == null) {
            Mercados mercadoExistente = mercadoRepository.findByName(dto.name);
            if (mercadoExistente != null) {
                return Response.status(Response.Status.CONFLICT).entity("Mercado já cadastrado").build();
            }

            Mercados mercado = new Mercados(dto.name, dto.endereco);
            mercadoRepository.persist(mercado);
            return Response.ok("Mercado criado com sucesso").build();
        }
        Mercados mercado = mercadoRepository.findById(dto.mercadoId);
        mercado.setNome(dto.name);
        mercado.setEndereco(dto.endereco);
        mercado.persist();
        return Response.ok("Mercado atualizado com sucesso").build();
    }
}