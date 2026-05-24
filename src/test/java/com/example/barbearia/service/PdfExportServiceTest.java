package com.example.barbearia.service;

import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.ComissaoLancamento;
import com.example.barbearia.domain.FolhaComissaoBarbeiro;
import com.example.barbearia.domain.StatusFolhaComissao;
import com.example.barbearia.dto.relatorio.ContagemPorStatusDTO;
import com.example.barbearia.dto.relatorio.RelatorioCompletoDTO;
import com.example.barbearia.domain.StatusAgendamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfExportServiceTest {

    private final PdfExportService pdfExportService = new PdfExportService();

    @Test
    void exportarComissoesVazioNaoFalha() {
        byte[] pdf = pdfExportService.exportarComissoes("Teste", List.of(), List.of());
        assertThat(pdf).isNotEmpty();
    }

    @Test
    void exportarComissoesSemBarbeiroCarregadoNaoFalha() {
        FolhaComissaoBarbeiro folha = FolhaComissaoBarbeiro.builder()
                .anoMes("2026-05")
                .status(StatusFolhaComissao.A_PAGAR)
                .valorTotal(new BigDecimal("50.00"))
                .quantidadeAtendimentos(1)
                .build();
        ComissaoLancamento lanc = ComissaoLancamento.builder()
                .valorComissao(new BigDecimal("20.00"))
                .percentualComissao(new BigDecimal("40"))
                .build();

        byte[] pdf = pdfExportService.exportarComissoes("Teste", List.of(folha), List.of(lanc));
        assertThat(pdf).isNotEmpty();
    }

    @Test
    void exportarComissoesComBarbeiroCarregado() {
        FolhaComissaoBarbeiro folha = FolhaComissaoBarbeiro.builder()
                .barbeiro(Barbeiro.builder().nome("Fabio").build())
                .anoMes("2026-05")
                .status(StatusFolhaComissao.A_PAGAR)
                .valorTotal(new BigDecimal("50.00"))
                .quantidadeAtendimentos(1)
                .build();

        byte[] pdf = pdfExportService.exportarComissoes("Teste", List.of(folha), List.of());
        assertThat(pdf).isNotEmpty();
    }

    @Test
    void exportarRelatorioCompletoMinimo() {
        RelatorioCompletoDTO dto = new RelatorioCompletoDTO(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 23),
                0, 0, 0, 0, 0, 0, 0,
                BigDecimal.ZERO,
                0, 0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(new ContagemPorStatusDTO(StatusAgendamento.AGENDADO, 0)),
                List.of(),
                List.of(),
                List.of()
        );
        byte[] pdf = pdfExportService.exportarRelatorioCompleto(dto);
        assertThat(pdf).isNotEmpty();
    }
}
