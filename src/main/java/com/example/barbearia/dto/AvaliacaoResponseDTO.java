package com.example.barbearia.dto;

import com.example.barbearia.domain.Avaliacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record AvaliacaoResponseDTO(
        UUID id,
        UUID agendamentoId,
        Integer nota,
        String comentario,
        String nomeCliente,
        LocalDateTime createdAt
) {
    public static AvaliacaoResponseDTO from(Avaliacao a) {
        UUID agendamentoId = a.getAgendamentoId();
        if (agendamentoId == null && a.getAgendamento() != null) {
            agendamentoId = a.getAgendamento().getId();
        }
        String nomeCliente = null;
        if (a.getAgendamento() != null && a.getAgendamento().getCliente() != null) {
            nomeCliente = a.getAgendamento().getCliente().getNome();
        }
        return new AvaliacaoResponseDTO(
                a.getId(),
                agendamentoId,
                a.getNota(),
                a.getComentario(),
                nomeCliente,
                a.getCreatedAt()
        );
    }
}
