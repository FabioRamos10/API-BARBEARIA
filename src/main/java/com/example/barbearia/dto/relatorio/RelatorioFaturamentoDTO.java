package com.example.barbearia.dto.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RelatorioFaturamentoDTO(
        LocalDate periodoInicio,
        LocalDate periodoFim,
        BigDecimal faturamentoServicosConcluidos
) {
}
