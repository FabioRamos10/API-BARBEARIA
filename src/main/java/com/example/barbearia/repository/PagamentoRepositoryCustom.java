package com.example.barbearia.repository;

import com.example.barbearia.domain.StatusPagamento;

import java.time.LocalDateTime;

public interface PagamentoRepositoryCustom {

    long countByAgendamentoInicioBetweenAndStatus(
            LocalDateTime desde,
            LocalDateTime ate,
            StatusPagamento status);
}
