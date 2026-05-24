package com.example.barbearia.dto.chatbot;

import java.util.List;

public record ChatbotResponseDTO(
        String resposta,
        String categoria,
        double confianca,
        List<String> sugestoes
) {
}
