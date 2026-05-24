package com.example.barbearia.repository;

import com.example.barbearia.domain.ComissaoLancamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ComissaoLancamentoRepositoryCustom {

    List<ComissaoLancamento> findByAnoMesComDetalhes(String anoMes);

    List<ComissaoLancamento> findByBarbeiroAndAnoMesComDetalhes(UUID barbeiroId, String anoMes);

    BigDecimal sumValorComissaoBetween(LocalDateTime desde, LocalDateTime ate);
}
