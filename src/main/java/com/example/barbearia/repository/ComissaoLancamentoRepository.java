package com.example.barbearia.repository;

import com.example.barbearia.domain.ComissaoLancamento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComissaoLancamentoRepository extends MongoRepository<ComissaoLancamento, UUID>, ComissaoLancamentoRepositoryCustom {

    Optional<ComissaoLancamento> findByAgendamentoId(UUID agendamentoId);

    default Optional<ComissaoLancamento> findByAgendamento_Id(UUID agendamentoId) {
        return findByAgendamentoId(agendamentoId);
    }

    List<ComissaoLancamento> findByBarbeiroIdAndAnoMesOrderByCreatedAtDesc(UUID barbeiroId, String anoMes);

    default List<ComissaoLancamento> findByBarbeiro_IdAndAnoMesOrderByCreatedAtDesc(UUID barbeiroId, String anoMes) {
        return findByBarbeiroIdAndAnoMesOrderByCreatedAtDesc(barbeiroId, anoMes);
    }

    List<ComissaoLancamento> findByAnoMesOrderByCreatedAtDesc(String anoMes);
}
