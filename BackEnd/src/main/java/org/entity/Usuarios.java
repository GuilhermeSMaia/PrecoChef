package org.entity;

import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuarios extends PanacheEntityBase {
    // valores de "role"; também são os grupos usados em @RolesAllowed
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String nome;

    // sempre gravado em minúsculas, para o login não depender de maiúsculas
    @Column(nullable = false, unique = true, length = 254)
    public String email;

    // hash bcrypt da senha; a senha em texto nunca é gravada
    @Column(nullable = false, length = 100)
    public String senhaHash;

    @Column(nullable = false, length = 20)
    public String role = ROLE_USER;

    public LocalDateTime criadoEm = LocalDateTime.now();

    public Usuarios() {}

    public Usuarios(String nome, String email, String senhaHash, String role) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.role = role;
    }
}
