package org.controlers.mercados;

import java.util.List;
import java.util.stream.Collectors;

import org.controlers.DTO.GetAllMercadosDTO;
import org.entity.Mercados;
import org.repository.MercadoRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/mercados")
public class GetMercados {
    @Inject
    MercadoRepository mercadoRepository;

    @GET
    @Path("/getAll")
    public Response getMercados() {
        try { 
            List<Mercados> mercados = mercadoRepository.listAll();
            List<GetAllMercadosDTO> dto = mercados.stream()
                .map(m -> new GetAllMercadosDTO(m.getId(), 
                        m.getNome(), 
                        m.getEndereco()))
                .collect(Collectors.toList());
            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    
}
