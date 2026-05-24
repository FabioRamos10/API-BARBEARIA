package com.example.barbearia.repository;

import com.example.barbearia.domain.Pagamento;
import com.example.barbearia.domain.StatusPagamento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoRepository extends MongoRepository<Pagamento, UUID>, PagamentoRepositoryCustom {

    List<Pagamento> findByStatus(StatusPagamento status);

    Optional<Pagamento> findByAgendamentoId(UUID agendamentoId);

    default Optional<Pagamento> findByAgendamento_Id(UUID agendamentoId) {
        return findByAgendamentoId(agendamentoId);
    }
}
