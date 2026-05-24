package com.example.barbearia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtrasoMensagemCreateDTO(
        @NotBlank @Size(max = 2000) String texto
) {
}
