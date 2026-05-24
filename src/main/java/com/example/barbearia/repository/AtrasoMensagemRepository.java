package com.example.barbearia.repository;

import com.example.barbearia.domain.AtrasoMensagem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AtrasoMensagemRepository extends MongoRepository<AtrasoMensagem, UUID> {

    List<AtrasoMensagem> findByAgendamentoIdOrderByCreatedAtAsc(UUID agendamentoId);

    default List<AtrasoMensagem> findByAgendamento_IdOrderByCreatedAtAsc(UUID agendamentoId) {
        return findByAgendamentoIdOrderByCreatedAtAsc(agendamentoId);
    }
}
