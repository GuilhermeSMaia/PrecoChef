package org.repository;

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
    
}
