package com.example.barbearia.repository;

import com.example.barbearia.domain.Cliente;

import java.util.Optional;

public interface ClienteRepositoryCustom {

    Optional<Cliente> findByUserEmail(String email);
}
