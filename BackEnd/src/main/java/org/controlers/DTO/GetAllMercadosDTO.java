package org.controlers.DTO;

public class GetAllMercadosDTO {
    public Long id;
    public String name;
    public String endereco;

    public GetAllMercadosDTO(Long id, String name, String endereco) {
        this.id = id;
        this.name = name;
        this.endereco = endereco;
    }
}