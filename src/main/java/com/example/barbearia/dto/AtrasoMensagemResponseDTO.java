package com.example.barbearia.dto;

import com.example.barbearia.domain.AtrasoMensagem;
import com.example.barbearia.domain.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record AtrasoMensagemResponseDTO(
        UUID id,
        UUID agendamentoId,
        UUID autorUserId,
        String autorNome,
        Role autorRole,
        String texto,
        boolean respostaOficial,
        LocalDateTime createdAt
) {
    public static AtrasoMensagemResponseDTO from(AtrasoMensagem m) {
        UUID agendamentoId = m.getAgendamentoId();
        if (agendamentoId == null && m.getAgendamento() != null) {
            agendamentoId = m.getAgendamento().getId();
        }
        return new AtrasoMensagemResponseDTO(
                m.getId(),
                agendamentoId,
                m.getAutorUserId(),
                m.getAutorNome(),
                m.getAutorRole(),
                m.getTexto(),
                m.isRespostaOficial(),
                m.getCreatedAt()
        );
    }
}
