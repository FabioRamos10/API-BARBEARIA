package com.example.barbearia.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record BarbeiroCreateDTO(
        @NotBlank String nome,
        String telefone,
        BigDecimal percentualComissao,
        String especialidades
) {
}
