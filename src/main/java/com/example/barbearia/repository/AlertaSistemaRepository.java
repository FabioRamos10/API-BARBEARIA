package com.example.barbearia.repository;

import com.example.barbearia.domain.AlertaSistema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertaSistemaRepository extends MongoRepository<AlertaSistema, UUID> {

    List<AlertaSistema> findByDestinatarioUserIdOrderByCreatedAtDesc(UUID destinatarioUserId);

    long countByDestinatarioUserIdAndLidoFalse(UUID destinatarioUserId);
}
