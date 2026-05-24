package com.example.barbearia.repository;

import com.example.barbearia.domain.Cliente;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends MongoRepository<Cliente, UUID>, ClienteRepositoryCustom {

    Optional<Cliente> findByEmail(String email);

    Optional<Cliente> findByEmailIgnoreCase(String email);

    Optional<Cliente> findByCpf(String cpf);

    Optional<Cliente> findByTelefone(String telefone);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID excludeId);

    boolean existsByTelefoneAndIdNot(String telefone, UUID excludeId);

    boolean existsByCpfAndIdNot(String cpf, UUID excludeId);

    boolean existsByCpf(String cpf);

    Optional<Cliente> findByUserId(UUID userId);
}
