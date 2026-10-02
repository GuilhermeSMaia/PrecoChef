package org.controlers.mercados;

import org.controlers.DTO.GetAllMercadosDTO;
import org.entity.Mercados;
import org.repository.MercadoRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/mercados")
public class GetDataById {

    @Inject MercadoRepository MercadoRepository;

    @GET
    @Path("/getDataById/{id}")
    public Response getMercadoData(@PathParam("id") Long id) {

        Mercados mercado = MercadoRepository.findById(id);
        GetAllMercadosDTO dto = new GetAllMercadosDTO(
            mercado.getId(), 
            mercado.getNome(), 
            mercado.getEndereco()
        );
        return Response.ok(dto).build();
    }
    
}
