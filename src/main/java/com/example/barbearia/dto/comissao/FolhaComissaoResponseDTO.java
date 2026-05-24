package com.example.barbearia.dto.comissao;

import com.example.barbearia.domain.FolhaComissaoBarbeiro;
import com.example.barbearia.domain.StatusFolhaComissao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FolhaComissaoResponseDTO(
        UUID id,
        UUID barbeiroId,
        String barbeiroNome,
        String anoMes,
        StatusFolhaComissao status,
        BigDecimal valorTotal,
        int quantidadeAtendimentos,
        LocalDateTime pagoEm,
        LocalDateTime updatedAt
) {
    public static FolhaComissaoResponseDTO from(FolhaComissaoBarbeiro f) {
        var barbeiro = f.getBarbeiro();
        return new FolhaComissaoResponseDTO(
                f.getId(),
                barbeiro != null ? barbeiro.getId() : f.getBarbeiroId(),
                barbeiro != null ? barbeiro.getNome() : "Barbeiro",
                f.getAnoMes(),
                f.getStatus(),
                f.getValorTotal(),
                f.getQuantidadeAtendimentos(),
                f.getPagoEm(),
                f.getUpdatedAt()
        );
    }
}
