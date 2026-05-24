package com.example.barbearia.dto;

import java.util.List;
import java.util.UUID;

public record AvaliacoesBarbeiroDTO(
        UUID barbeiroId,
        String nomeBarbeiro,
        Double mediaNotas,
        long totalAvaliacoes,
        List<AvaliacaoResponseDTO> itens
) {
}
