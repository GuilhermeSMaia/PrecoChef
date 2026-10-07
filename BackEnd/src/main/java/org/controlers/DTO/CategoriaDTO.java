package org.controlers.DTO;

public class CategoriaDTO {
    public Long id;
    public String nome;
    public long quantidadeProdutos;

    public CategoriaDTO(Long id, String nome, long quantidadeProdutos) {
        this.id = id;
        this.nome = nome;
        this.quantidadeProdutos = quantidadeProdutos;
    }
}
