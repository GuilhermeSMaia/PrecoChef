package org.repository;

import org.entity.Medidas;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MedidasRepository implements PanacheRepositoryBase<Medidas, Long> {

    public Medidas findByMedida(String medida) {
        return find("medida", medida).firstResult();
    }

    public Medidas findOrCreate(String medida) {
        Medidas existente = find("lower(medida) = lower(?1)", medida.trim()).firstResult();
        if (existente != null) return existente;

        Medidas nova = new Medidas(medida.trim());
        persist(nova);
        return nova;
    }

    
}
