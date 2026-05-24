package com.example.barbearia.repository;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.Cliente;
import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.StatusAgendamento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgendamentoRepository extends MongoRepository<Agendamento, UUID>, AgendamentoRepositoryCustom {

    default List<Agendamento> findByClienteOrderByInicioDesc(Cliente cliente) {
        return findByClienteIdOrderByInicioDesc(cliente.getId());
    }

    List<Agendamento> findByClienteIdOrderByInicioDesc(UUID clienteId);

    default List<Agendamento> findByBarbeiroOrderByInicioDesc(Barbeiro barbeiro) {
        return findByBarbeiroIdOrderByInicioDesc(barbeiro.getId());
    }

    List<Agendamento> findByBarbeiroIdOrderByInicioDesc(UUID barbeiroId);

    List<Agendamento> findByStatus(StatusAgendamento status);
}
