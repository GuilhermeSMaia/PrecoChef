package org.entity;

import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Lembra que um item de um mercado (pelo nome que o site usa) é um produto já cadastrado com outro nome.
 * Criado quando a sincronização liga produtos por nome (mercados sem código de barras, ex.: Meta 21):
 * nas coletas seguintes o item vai direto para o produto ligado, em vez de ser cadastrado de novo.
 */
@Entity
@Table(name = "produtos_vinculos", uniqueConstraints = @UniqueConstraint(columnNames = { "mercado_id", "chave" }))
public class VinculosProdutos extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mercado_id", nullable = false)
    public Mercados mercado;

    // nome do item no site (minúsculo) + "|" + id da medida: a mesma chave que a sincronização usa
    @Column(nullable = false, length = 400)
    public String chave;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    public Produtos produto;

    public LocalDateTime criadoEm = LocalDateTime.now();

    public VinculosProdutos() {}

    public VinculosProdutos(Mercados mercado, String chave, Produtos produto) {
        this.mercado = mercado;
        this.chave = chave;
        this.produto = produto;
    }
}
