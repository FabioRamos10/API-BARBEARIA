package com.example.barbearia.dto;

import com.example.barbearia.domain.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PagamentoCreateDTO(
        @NotNull UUID agendamentoId,
        @NotNull FormaPagamento formaPagamento,
        @Positive BigDecimal valor,
        Boolean confirmarImediato,
        String observacao
) {
}
