package com.example.barbearia.dto;

import java.util.UUID;

public record UsuarioChatDTO(
        UUID userId,
        String nome,
        String email,
        String role
) {
}
