package org.controlers.DTO;

import java.util.List;

import org.entity.Mercados;

public class GetAllMercadosDTO {
    public Long id;
    public String name;
    public String url;
    public List<EnderecoDTO> enderecos;

    public GetAllMercadosDTO(Mercados mercado) {
        this.id = mercado.getId();
        this.name = mercado.getNome();
        this.url = mercado.getUrl();
        this.enderecos = mercado.getEnderecos().stream().map(EnderecoDTO::new).toList();
    }
}
