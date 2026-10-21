package org.controlers.produtos;

import org.controlers.DTO.CreateProdutoDTO;
import org.entity.Categoria;
import org.entity.Medidas;
import org.entity.Produtos;
import org.entity.Usuarios;
import org.repository.CategoriaRepository;
import org.repository.MedidasRepository;
import org.repository.ProdutosRepository;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;


@Path("/produtos")
// só admin altera dados (o token JWT precisa ter o grupo "admin")
@RolesAllowed(Usuarios.ROLE_ADMIN)
public class CreateProduto {
    @Inject
    ProdutosRepository produtoRepository;
    @Inject
    MedidasRepository medidasRepository;
    @Inject
    CategoriaRepository categoriaRepository;
    
    @POST
    @Transactional
    @Path("/create")
    public Response createProdutos(CreateProdutoDTO dto) {
        Categoria categoria = categoriaRepository.findByCategoria(dto.categoria);
        if (categoria == null) {
            return Response.status(Response.Status.CONFLICT).entity("Categoria não cadastrada").build();
            
        }
        Medidas medida = medidasRepository.findByMedida(dto.medida);
        if (medida == null) {
            return Response.status(Response.Status.CONFLICT).entity("Medida não cadastrada").build();
        }
        if (dto.produtoId == null) {
            Produtos produtoExistente = produtoRepository.findByNomeAndMedida(dto.nome, medida.id);
            if (produtoExistente == null) {
                Produtos produto = new Produtos(dto.nome, medida, categoria);
                produtoRepository.persist(produto);
                return Response.ok("Produto criado com sucesso").build();
            }
            return Response.status(Response.Status.CONFLICT).entity("Produto já cadastrado com mesmo nome e medida").build();

        }

        Produtos produto = produtoRepository.findById(dto.produtoId);  
        produto.setNome(dto.nome);
        produto.setCategoria(categoria);
        produto.setMedidas(medida);
        produto.persist();
        return Response.ok("Produto atualizado com sucesso").build();

    }

    
}
