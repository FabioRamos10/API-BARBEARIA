package com.example.barbearia.repository;

import com.example.barbearia.domain.Avaliacao;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvaliacaoRepository extends MongoRepository<Avaliacao, UUID>, AvaliacaoRepositoryCustom {

    Optional<Avaliacao> findByAgendamentoId(UUID agendamentoId);

    default Optional<Avaliacao> findByAgendamento_Id(UUID agendamentoId) {
        return findByAgendamentoId(agendamentoId);
    }

    boolean existsByAgendamentoId(UUID agendamentoId);

    default boolean existsByAgendamento_Id(UUID agendamentoId) {
        return existsByAgendamentoId(agendamentoId);
    }
}
