package org.entity;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "mercados")
public class Mercados extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;

    // um mercado pode ter várias lojas/endereços; salvar o mercado salva/remove os endereços junto
    @OneToMany(mappedBy = "mercado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enderecos> enderecos = new ArrayList<>();

    @OneToMany(mappedBy = "mercados")
    private List<Precos> precos;

    public Mercados() {}
    public Mercados(String nome) {
        this.nome = nome;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public List<Enderecos> getEnderecos() { return enderecos; }
    public List<Precos> getPrecos() { return precos; }

    public void adicionarEndereco(String descricao, String endereco) {
        enderecos.add(new Enderecos(descricao, endereco, this));
    }
}
