package org.repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // todos os preços de um mercado (usado pelo webscraping para atualizar sem consultar item a item)
    public List<Precos> listByMercado(Mercados mercados) {
        return find("select pr from Precos pr join fetch pr.produtos where pr.mercados = ?1", mercados).list();
    }

    // id do produto -> ids dos mercados em que ele tem preço (uma consulta só, sem carregar entidades)
    public Map<Long, Set<Long>> mercadosPorProduto() {
        Map<Long, Set<Long>> resultado = new HashMap<>();
        getEntityManager()
                .createQuery("select pr.produtos.id, pr.mercados.id from Precos pr", Object[].class)
                .getResultList()
                .forEach(l -> resultado.computeIfAbsent((Long) l[0], k -> new HashSet<>()).add((Long) l[1]));
        return resultado;
    }

    // preços de vários produtos de uma vez (evita uma consulta por produto), do menor para o maior
    public List<Precos> listByProdutos(List<Long> produtosIds) {
        if (produtosIds.isEmpty()) return List.of();
        return find("select pr from Precos pr join fetch pr.mercados where pr.produtos.id in ?1 order by pr.preco",
                produtosIds).list();
    }
}
