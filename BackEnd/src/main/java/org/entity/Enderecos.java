package org.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// cada unidade/loja de um mercado (ex.: Irmãos Gonçalves - Centro, Irmãos Gonçalves - Jatuaiba)
@Entity
@Table(name = "enderecos")
public class Enderecos extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao; // opcional, ex.: "Loja Centro"
    private String endereco;

    @ManyToOne
    @JoinColumn(name = "mercados_id")
    @JsonbTransient
    private Mercados mercado;

    public Enderecos() {}
    public Enderecos(String descricao, String endereco, Mercados mercado) {
        this.descricao = descricao;
        this.endereco = endereco;
        this.mercado = mercado;
    }

    public Long getId() { return id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public Mercados getMercado() { return mercado; }
    public void setMercado(Mercados mercado) { this.mercado = mercado; }
}
