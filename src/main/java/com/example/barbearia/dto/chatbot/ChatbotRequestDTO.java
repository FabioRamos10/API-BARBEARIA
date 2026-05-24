package com.example.barbearia.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotRequestDTO(
        @NotBlank @Size(max = 1000) String mensagem
) {
}
