package com.example.barbearia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgendamentoAtrasoDTO(
        @NotNull @Min(1) @Max(180) Integer minutos,
        @NotBlank @Size(max = 500) String motivo
) {
}
