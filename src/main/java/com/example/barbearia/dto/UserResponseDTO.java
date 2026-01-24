package com.example.barbearia.dto;

import com.example.barbearia.domain.Role;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String nome,
        String email,
        Role role,
        Boolean ativo
) {}
