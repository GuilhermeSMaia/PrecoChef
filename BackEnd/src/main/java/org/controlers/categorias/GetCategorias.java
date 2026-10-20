package org.controlers.categorias;

import java.util.List;

import org.repository.CategoriaRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

// categorias cadastradas, em ordem alfabética, com quantos produtos cada uma tem (filtro de produtos e cadastro de produto)
@Path("/categorias")
public class GetCategorias {
    @Inject
    CategoriaRepository categoriaRepository;

    public record CategoriaDTO(String nome, long produtos) {}

    @GET
    public Response getCategorias() {
        // uma única consulta agrupada, sem carregar os produtos
        List<CategoriaDTO> categorias = categoriaRepository.getEntityManager()
                .createQuery("select c.categoria, count(p) from Categoria c left join c.produtos p"
                        + " where c.categoria is not null and trim(c.categoria) <> ''"
                        + " group by c.id, c.categoria order by c.categoria", Object[].class)
                .getResultList().stream()
                .map(linha -> new CategoriaDTO((String) linha[0], (Long) linha[1]))
                .toList();
        return Response.ok(categorias).build();
    }
}
