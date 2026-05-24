package com.example.barbearia.repository;

import com.example.barbearia.domain.Servico;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicoRepository extends MongoRepository<Servico, UUID> {

    List<Servico> findByAtivoTrue();

    List<Servico> findByCategoriaAndAtivoTrue(String categoria);
}
