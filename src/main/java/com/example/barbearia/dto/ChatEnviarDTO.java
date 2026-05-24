package com.example.barbearia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatEnviarDTO(
        @Size(max = 4000) String texto
) {
}
