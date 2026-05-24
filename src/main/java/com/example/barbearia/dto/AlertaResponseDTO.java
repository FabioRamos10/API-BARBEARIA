package com.example.barbearia.dto;

import com.example.barbearia.domain.AlertaSistema;
import com.example.barbearia.domain.TipoAlerta;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertaResponseDTO(
        UUID id,
        String titulo,
        String mensagem,
        TipoAlerta tipo,
        UUID referenciaId,
        boolean lido,
        LocalDateTime createdAt
) {
    public static AlertaResponseDTO from(AlertaSistema a) {
        return new AlertaResponseDTO(
                a.getId(), a.getTitulo(), a.getMensagem(), a.getTipo(),
                a.getReferenciaId(), a.isLido(), a.getCreatedAt()
        );
    }
}
