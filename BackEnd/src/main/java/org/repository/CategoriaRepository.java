package org.repository;

import java.util.List;

import org.controlers.DTO.CategoriaDTO;
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

    public List<CategoriaDTO> listarComQuantidade() {
        return getEntityManager().createQuery(
                "select new org.controlers.DTO.CategoriaDTO(c.id, c.categoria, count(p.id)) "
                        + "from Categoria c left join c.produtos p "
                        + "group by c.id, c.categoria order by c.categoria",
                CategoriaDTO.class).getResultList();
    }
}
