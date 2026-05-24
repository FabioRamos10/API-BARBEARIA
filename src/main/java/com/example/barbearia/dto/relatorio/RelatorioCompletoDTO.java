package com.example.barbearia.dto.relatorio;

import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.domain.StatusFolhaComissao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RelatorioCompletoDTO(
        LocalDate inicio,
        LocalDate fim,
        long totalAgendamentos,
        long cortesConcluidos,
        long cancelados,
        long faltas,
        long emAndamento,
        long agendadosConfirmados,
        long atrasosInformados,
        BigDecimal faturamentoConcluidos,
        long pagamentosPendentes,
        long pagamentosPagos,
        BigDecimal totalComissoesPeriodo,
        BigDecimal comissaoAPagar,
        BigDecimal comissaoEmAndamento,
        BigDecimal comissaoPago,
        List<ItemFolhaComissaoDTO> folhasComissao,
        List<ContagemPorStatusDTO> porStatus,
        List<ItemServicoDTO> porServico,
        List<ItemBarbeiroDetalheDTO> porBarbeiro,
        List<ItemBarbeiroStatusDTO> barbeiroPorStatus
) {
    public record ItemFolhaComissaoDTO(
            UUID folhaId,
            UUID barbeiroId,
            String barbeiroNome,
            String anoMes,
            StatusFolhaComissao status,
            BigDecimal valorTotal,
            int quantidadeAtendimentos,
            LocalDateTime pagoEm
    ) {
    }

    public record ItemServicoDTO(UUID servicoId, String servicoNome, long quantidade) {
    }

    public record ItemBarbeiroDetalheDTO(
            UUID barbeiroId,
            String barbeiroNome,
            long total,
            long concluidos,
            long cancelados,
            long faltas
    ) {
    }

    public record ItemBarbeiroStatusDTO(
            UUID barbeiroId,
            String barbeiroNome,
            StatusAgendamento status,
            long quantidade
    ) {
    }
}
