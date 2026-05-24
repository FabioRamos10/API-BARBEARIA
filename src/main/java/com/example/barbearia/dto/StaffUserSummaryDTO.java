package com.example.barbearia.dto;

import com.example.barbearia.domain.Role;

import java.util.UUID;

public record StaffUserSummaryDTO(
        UUID userId,
        UUID barbeiroId,
        String nome,
        String email,
        String telefone,
        Role role,
        boolean ativo
) {
}
