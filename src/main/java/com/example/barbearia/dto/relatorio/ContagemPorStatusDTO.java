package com.example.barbearia.dto.relatorio;

import com.example.barbearia.domain.StatusAgendamento;

public record ContagemPorStatusDTO(
        StatusAgendamento status,
        long quantidade
) {
}
