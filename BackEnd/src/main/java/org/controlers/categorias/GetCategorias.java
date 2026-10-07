package org.controlers.categorias;

import java.util.List;

import org.controlers.DTO.CategoriaDTO;
import org.repository.CategoriaRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

// lista as categorias cadastradas (inclusive as criadas pelo webscraping), com a quantidade de produtos
@Path("/categorias")
public class GetCategorias {
    @Inject
    CategoriaRepository categoriaRepository;

    @GET
    public Response getCategorias() {
        List<CategoriaDTO> dto = categoriaRepository.listarComQuantidade();
        return Response.ok(dto).build();
    }
}
