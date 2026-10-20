package org.controlers.DTO;

import org.entity.Enderecos;

public class EnderecoDTO {
    public Long id;          // preencher ao editar um endereço existente; vazio = endereço novo
    public String descricao; // opcional, ex.: "Loja Centro"
    public String endereco;

    public EnderecoDTO() {}

    public EnderecoDTO(Enderecos e) {
        this.id = e.getId();
        this.descricao = e.getDescricao();
        this.endereco = e.getEndereco();
    }
}
