package com.example.barbearia.repository;

import com.example.barbearia.domain.Avaliacao;
import com.example.barbearia.domain.Barbeiro;

import java.util.List;
import java.util.UUID;

public interface AvaliacaoRepositoryCustom {

    Double calcularMediaAvaliacoesPorBarbeiro(UUID barbeiroId);

    List<Avaliacao> findByBarbeiro(Barbeiro barbeiro);
}
