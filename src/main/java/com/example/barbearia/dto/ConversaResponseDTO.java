package com.example.barbearia.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversaResponseDTO(
        UUID conversaId,
        UUID outroUserId,
        String outroNome,
        String outroEmail,
        String outroRole,
        LocalDateTime ultimaMensagemEm,
        long mensagensNaoLidas
) {
}
