package com.example.barbearia.repository;

import com.example.barbearia.domain.Conversa;

import java.util.List;
import java.util.UUID;

public interface ConversaRepositoryCustom {

    List<Conversa> findByParticipante(UUID userId);
}
