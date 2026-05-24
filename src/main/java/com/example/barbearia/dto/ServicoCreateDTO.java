package com.example.barbearia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ServicoCreateDTO(
        @NotBlank String nome,
        String descricao,
        @NotNull @Positive BigDecimal preco,
        @NotNull @Positive Integer duracaoMinutos,
        String categoria
) {
}
