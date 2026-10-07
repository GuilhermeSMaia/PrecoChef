package org.repository;

import java.util.List;

import org.entity.Mercados;
import org.entity.Precos;
import org.entity.Produtos;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
@ApplicationScoped
public class PrecosRepository implements PanacheRepositoryBase<Precos, Long> {

    public Precos findByMercadosAndProdutos(Mercados mercados, Produtos produtos) {
    return find("mercados = ?1 and produtos = ?2", mercados, produtos).firstResult();
}

    // preços de vários produtos de uma vez (evita uma consulta por produto), do menor para o maior
    public List<Precos> listByProdutos(List<Long> produtosIds) {
        if (produtosIds.isEmpty()) return List.of();
        return find("select pr from Precos pr join fetch pr.mercados where pr.produtos.id in ?1 order by pr.preco",
                produtosIds).list();
    }
}
