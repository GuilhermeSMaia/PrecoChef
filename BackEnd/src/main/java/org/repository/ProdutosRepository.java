package org.repository;

import java.util.List;

import org.entity.Produtos;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProdutosRepository implements PanacheRepositoryBase<Produtos, Long> {

   
    public Produtos findByNome(String Nome) {
        return find("nome", Nome).firstResult();
    }
    
    public Produtos findByNomeAndMedida(String nome, Long medidasId) {
        return find("nome = ?1 and medidas.id = ?2", nome, medidasId).firstResult();
    }

    // usado pelo webscraping para montar o cache nome+medida numa única consulta
    public List<Produtos> listAllComMedida() {
        return find("select p from Produtos p join fetch p.medidas left join fetch p.categoria").list();
    }
}
