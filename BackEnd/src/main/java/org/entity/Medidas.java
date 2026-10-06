package org.entity;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "medidas")
public class Medidas extends PanacheEntityBase{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String medida;

    @OneToMany (mappedBy = "medidas")
    public List<Produtos> produtos;

    public Medidas() {}

    public Medidas(String medida) {
        this.medida = medida;
    }

    public String getMedida() {
        return medida;
    }
}
