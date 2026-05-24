package com.example.barbearia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BarbeiroUpdateDTO(
        String telefone,
        BigDecimal percentualComissao,
        String especialidades
) {
}
