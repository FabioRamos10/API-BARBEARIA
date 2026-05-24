package com.example.barbearia.dto;

import com.example.barbearia.domain.Role;

import java.util.UUID;

public record StaffUserResponseDTO(
        UUID userId,
        UUID barbeiroId,
        String nome,
        String email,
        Role role,
        boolean ativo
) {
}
