package com.example.barbearia.repository;

import com.example.barbearia.domain.Barbeiro;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarbeiroRepository extends MongoRepository<Barbeiro, UUID>, BarbeiroRepositoryCustom {

    List<Barbeiro> findByAtivoTrue();

    Optional<Barbeiro> findByUserId(UUID userId);

    default Optional<Barbeiro> findByUser_Id(UUID userId) {
        return findByUserId(userId);
    }

    boolean existsByTelefoneAndIdNot(String telefone, UUID excludeId);
}
