package org.entity;

import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

// a lista de compras mais recente salva por cada usuário (uma por usuário)
@Entity
@Table(name = "listas_compras")
public class ListasCompras extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    public Usuarios usuario;

    @Column(length = 100)
    public String nome;

    // itens como o front os guarda (produto, preço, mercado e quantidade), em JSON:
    // é uma "foto" da lista no momento em que foi salva, inclusive dos preços
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    public String itens;

    public LocalDateTime atualizadaEm = LocalDateTime.now();
}
