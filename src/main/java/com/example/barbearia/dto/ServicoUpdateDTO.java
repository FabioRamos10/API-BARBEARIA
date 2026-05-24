package com.example.barbearia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServicoUpdateDTO(
        String nome,
        String descricao,
        BigDecimal preco,
        Integer duracaoMinutos,
        String categoria,
        Boolean ativo
) {
}
