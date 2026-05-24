package com.example.barbearia.repository;

import com.example.barbearia.domain.Conversa;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversaRepository extends MongoRepository<Conversa, UUID>, ConversaRepositoryCustom {

    Optional<Conversa> findByUsuarioMenorIdAndUsuarioMaiorId(UUID menor, UUID maior);
}
