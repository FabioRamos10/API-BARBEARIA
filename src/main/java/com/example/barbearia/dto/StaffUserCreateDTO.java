package com.example.barbearia.dto;

import com.example.barbearia.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StaffUserCreateDTO(
        @NotBlank String nome,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 120) String senha,
        @NotNull Role role,
        String telefone,
        BigDecimal percentualComissao,
        String especialidades
) {
}
