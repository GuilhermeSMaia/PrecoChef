package org.controlers.mercados;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.controlers.DTO.PaginaDTO;
import org.controlers.DTO.ProdutoDoMercadoDTO;
import org.entity.Precos;
import org.repository.MercadoRepository;
import org.repository.PrecosRepository;

import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

// produtos de um mercado, paginados no banco. Ex.: GET /mercados/1/produtos?pagina=0&tamanho=10&busca=margarina&ordenar=price-asc
@Path("/mercados")
public class GetProdutosDoMercado {
    private static final int TAMANHO_MAXIMO = 100;

    @Inject
    MercadoRepository mercadoRepository;
    @Inject
    PrecosRepository precosRepository;

    @GET
    @Path("/{id}/produtos")
    public Response produtosDoMercado(
            @PathParam("id") Long id,
            @QueryParam("pagina") @DefaultValue("0") int pagina,
            @QueryParam("tamanho") @DefaultValue("10") int tamanho,
            @QueryParam("busca") String busca,
            @QueryParam("ordenar") String ordenar) {

        if (mercadoRepository.findById(id) == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Mercado não encontrado").build();
        }

        pagina = Math.max(pagina, 0);
        tamanho = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);

        StringBuilder where = new StringBuilder(" where pr.mercados.id = :mercado");
        Map<String, Object> params = new HashMap<>();
        params.put("mercado", id);
        if (busca != null && !busca.isBlank()) {
            where.append(" and lower(pr.produtos.nome) like :busca");
            params.put("busca", "%" + busca.trim().toLowerCase() + "%");
        }

        String orderBy = switch (ordenar == null ? "" : ordenar) {
            case "price-asc" -> " order by pr.preco asc, pr.produtos.nome";
            case "price-desc" -> " order by pr.preco desc, pr.produtos.nome";
            default -> " order by pr.produtos.nome";
        };

        long total = precosRepository.count("from Precos pr" + where, params);

        // join fetch só em relações "para um": a paginação continua sendo feita no banco (limit/offset)
        List<Precos> precos = precosRepository
                .find("select pr from Precos pr join fetch pr.produtos p left join fetch p.categoria" + where + orderBy, params)
                .page(Page.of(pagina, tamanho))
                .list();

        List<ProdutoDoMercadoDTO> itens = precos.stream()
                .map(pr -> new ProdutoDoMercadoDTO(
                        pr.produtos.id,
                        pr.produtos.nome,
                        pr.produtos.categoria != null ? pr.produtos.categoria.getCategoria() : null,
                        pr.preco,
                        pr.ultimaAtualizacao))
                .toList();

        return Response.ok(new PaginaDTO<>(itens, pagina, tamanho, total)).build();
    }
}
