package com.example.barbearia.repository;

import com.example.barbearia.domain.Barbeiro;

import java.util.Optional;

public interface BarbeiroRepositoryCustom {

    Optional<Barbeiro> findByUserEmail(String email);
}
