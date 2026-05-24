package com.example.barbearia.dto.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioResumoDTO(
        LocalDate periodoInicio,
        LocalDate periodoFim,
        long totalAgendamentos,
        List<ContagemPorStatusDTO> porStatus,
        BigDecimal taxaConclusaoPercent,
        BigDecimal taxaCancelamentoPercent,
        BigDecimal taxaFaltaPercent
) {
}
