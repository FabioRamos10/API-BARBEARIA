package com.example.barbearia.dto;

import com.example.barbearia.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequestDTO(
        @NotBlank String nome,
        @Email @NotBlank String email,
        @NotBlank String senha,
        @NotNull Role role
) {}
