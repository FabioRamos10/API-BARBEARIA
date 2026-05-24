package com.example.barbearia.repository;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgendamentoRepositoryCustom {

    List<Agendamento> findByBarbeiroAndPeriodo(
            Barbeiro barbeiro,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<StatusAgendamento> statuses);

    boolean existsConflitoHorario(
            UUID barbeiroId,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<StatusAgendamento> statuses);

    List<Agendamento> findAgendamentosParaLembrete(
            LocalDateTime inicio,
            LocalDateTime fim,
            List<StatusAgendamento> statuses);

    List<Object[]> countGroupedByStatusBetween(LocalDateTime desde, LocalDateTime ate);

    List<Object[]> countGroupedByBarbeiroBetween(LocalDateTime desde, LocalDateTime ate);

    long countBetween(LocalDateTime desde, LocalDateTime ate);

    BigDecimal sumPrecoServicosConcluidosBetween(
            LocalDateTime desde,
            LocalDateTime ate,
            StatusAgendamento statusConcluido);

    List<Agendamento> findParaLembreteNaJanela(
            List<StatusAgendamento> statuses,
            LocalDateTime inicio,
            LocalDateTime fim);

    Optional<Agendamento> findByIdComDetalhes(UUID id);

    List<Object[]> countGroupedByServicoBetween(LocalDateTime desde, LocalDateTime ate);

    long countAtrasosInformadosBetween(LocalDateTime desde, LocalDateTime ate);

    List<Object[]> countGroupedByBarbeiroAndStatusBetween(LocalDateTime desde, LocalDateTime ate);

    List<Agendamento> findSubsequentesBarbeiro(
            UUID barbeiroId,
            LocalDateTime inicioMin,
            List<StatusAgendamento> statuses,
            UUID excluirId);

    List<Agendamento> findOcupadosBarbeiroNoPeriodo(
            UUID barbeiroId,
            LocalDateTime periodoInicio,
            LocalDateTime periodoFim,
            List<StatusAgendamento> statuses);

    List<Agendamento> findConcluidosNoPeriodo(LocalDateTime desde, LocalDateTime ate);

    List<Agendamento> findAgendamentosNoPeriodo(LocalDateTime desde, LocalDateTime ate);
}
