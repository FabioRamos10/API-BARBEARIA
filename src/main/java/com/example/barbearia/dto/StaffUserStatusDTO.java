package com.example.barbearia.dto;

import jakarta.validation.constraints.NotNull;

public record StaffUserStatusDTO(
        @NotNull Boolean ativo
) {
}
