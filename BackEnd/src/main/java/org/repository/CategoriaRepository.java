package org.repository;

import org.entity.Categoria;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoriaRepository implements PanacheRepositoryBase<Categoria, Long> {

    public Categoria findByCategoria(String categoria) {
        return find("categoria", categoria).firstResult();
    }

    // usado pelo webscraping: reaproveita a categoria existente (sem diferenciar maiúsculas) ou cadastra uma nova
    public Categoria findOrCreate(String categoria) {
        Categoria existente = find("lower(categoria) = lower(?1)", categoria.trim()).firstResult();
        if (existente != null) return existente;

        Categoria nova = new Categoria(categoria.trim());
        persist(nova);
        return nova;
    }


    
}
