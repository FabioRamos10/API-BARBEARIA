package com.example.barbearia.repository;

import com.example.barbearia.domain.Mensagem;
import com.example.barbearia.domain.StatusMensagem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensagemRepository extends MongoRepository<Mensagem, UUID> {

    List<Mensagem> findByStatus(StatusMensagem status);

    List<Mensagem> findByStatusOrderByCreatedAtAsc(StatusMensagem status, Pageable pageable);

    boolean existsByAgendamentoIdAndConteudoStartingWith(UUID agendamentoId, String prefix);

    boolean existsByAgendamentoIdAndDestinatarioIgnoreCaseAndConteudoStartingWith(
            UUID agendamentoId,
            String destinatario,
            String prefix);

    default boolean existsByAgendamento_IdAndConteudoStartingWith(UUID agendamentoId, String prefix) {
        return existsByAgendamentoIdAndConteudoStartingWith(agendamentoId, prefix);
    }

    default boolean existsByAgendamento_IdAndDestinatarioIgnoreCaseAndConteudoStartingWith(
            UUID agendamentoId,
            String destinatario,
            String prefix) {
        return existsByAgendamentoIdAndDestinatarioIgnoreCaseAndConteudoStartingWith(
                agendamentoId, destinatario, prefix);
    }
}
