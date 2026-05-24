package com.example.barbearia.dto;

import com.example.barbearia.domain.FormaPagamento;
import com.example.barbearia.domain.Pagamento;
import com.example.barbearia.domain.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagamentoResponseDTO(
        UUID id,
        UUID agendamentoId,
        FormaPagamento formaPagamento,
        BigDecimal valor,
        StatusPagamento status,
        String pixChave,
        String pixCopiaCola,
        String pixQrCodeUrl,
        boolean comprovanteEnviado,
        LocalDateTime comprovanteEnviadoEm,
        LocalDateTime dataPagamento,
        String observacao,
        LocalDateTime createdAt
) {
    public static PagamentoResponseDTO from(Pagamento p, String pixChaveExibicao) {
        UUID agendamentoId = p.getAgendamentoId();
        if (agendamentoId == null && p.getAgendamento() != null) {
            agendamentoId = p.getAgendamento().getId();
        }
        return new PagamentoResponseDTO(
                p.getId(),
                agendamentoId,
                p.getFormaPagamento(),
                p.getValor(),
                p.getStatus(),
                pixChaveExibicao,
                p.getPixCopiaCola(),
                p.getPixQrCodeUrl(),
                p.getComprovantePath() != null && !p.getComprovantePath().isBlank(),
                p.getComprovanteEnviadoEm(),
                p.getDataPagamento(),
                p.getObservacao(),
                p.getCreatedAt()
        );
    }
}
