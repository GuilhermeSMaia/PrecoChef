package org.controlers.produtos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.controlers.DTO.GetAllProdutosDTO;
import org.controlers.DTO.PaginaDTO;
import org.controlers.DTO.PrecosInfoDTO;
import org.entity.Precos;
import org.entity.Produtos;
import org.repository.PrecosRepository;
import org.repository.ProdutosRepository;

import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

// listagem paginada para a tela de produtos. Ex.: GET /produtos/paginado?pagina=0&tamanho=24&busca=arroz&categoria=Mercearia&ordenar=price-asc
@Path("/produtos")
public class GetPaginado {
    private static final int TAMANHO_MAXIMO = 100;
    private static final String MENOR_PRECO = "(select min(pr.preco) from Precos pr where pr.produtos = p)";

    @Inject
    ProdutosRepository produtosRepository;
    @Inject
    PrecosRepository precosRepository;

    @GET
    @Path("/paginado")
    public Response paginado(
            @QueryParam("pagina") @DefaultValue("0") int pagina,
            @QueryParam("tamanho") @DefaultValue("24") int tamanho,
            @QueryParam("busca") String busca,
            @QueryParam("categoria") String categoria,
            @QueryParam("ordenar") String ordenar) {

        pagina = Math.max(pagina, 0);
        tamanho = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);

        // filtros aplicados no banco, não no navegador
        StringBuilder where = new StringBuilder("from Produtos p where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (busca != null && !busca.isBlank()) {
            where.append(" and lower(p.nome) like :busca");
            params.put("busca", "%" + busca.trim().toLowerCase() + "%");
        }
        if (categoria != null && !categoria.isBlank()) {
            where.append(" and lower(p.categoria.categoria) = :categoria");
            params.put("categoria", categoria.trim().toLowerCase());
        }

        long total = produtosRepository.count(where.toString(), params);

        String orderBy = switch (ordenar == null ? "" : ordenar) {
            case "price-asc" -> " order by " + MENOR_PRECO + " asc nulls last, p.nome";
            case "price-desc" -> " order by " + MENOR_PRECO + " desc nulls last, p.nome";
            default -> " order by p.nome";
        };

        List<Produtos> produtos = produtosRepository.find(where + orderBy, params)
                .page(Page.of(pagina, tamanho))
                .list();

        // busca os preços só dos produtos desta página, numa única consulta
        Map<Long, List<Precos>> precosPorProduto = precosRepository
                .listByProdutos(produtos.stream().map(Produtos::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(pr -> pr.getProdutos().getId()));

        List<GetAllProdutosDTO> itens = produtos.stream()
                .map(p -> new GetAllProdutosDTO(
                        p.getId(),
                        p.getNome(),
                        p.getMedidas() != null ? p.getMedidas().getMedida() : null,
                        p.getCategoria() != null ? p.getCategoria().getCategoria() : null,
                        precosPorProduto.getOrDefault(p.getId(), List.of()).stream()
                                .map(pr -> new PrecosInfoDTO(pr.getPreco(), pr.getMercados().getNome(), pr.getUltimaAtualizacao()))
                                .toList()))
                .toList();

        return Response.ok(new PaginaDTO<>(itens, pagina, tamanho, total)).build();
    }
}
