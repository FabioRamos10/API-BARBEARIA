package com.example.barbearia.service;



import com.example.barbearia.domain.Agendamento;

import com.example.barbearia.domain.ComissaoLancamento;

import com.example.barbearia.domain.FolhaComissaoBarbeiro;

import com.example.barbearia.dto.relatorio.RelatorioCompletoDTO;

import com.example.barbearia.pdf.PdfBrand;

import com.example.barbearia.pdf.PdfFonts;

import com.example.barbearia.pdf.PdfLetterheadEvent;

import com.example.barbearia.pdf.PdfStyledTable;

import com.lowagie.text.Document;

import com.lowagie.text.Paragraph;

import com.lowagie.text.pdf.PdfPTable;

import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;



import java.io.ByteArrayOutputStream;

import java.math.BigDecimal;

import java.time.format.DateTimeFormatter;

import java.util.List;



@Service

public class PdfExportService {



    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");



    public byte[] exportarComissoes(String titulo, List<FolhaComissaoBarbeiro> folhas, List<ComissaoLancamento> lancamentos) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = PdfLetterheadEvent.newDocument();

            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            writer.setPageEvent(new PdfLetterheadEvent("Folha de comissões"));

            doc.open();



            PdfLetterheadEvent.addCoverBlock(

                    doc,

                    titulo,

                    PdfBrand.NAME,

                    folhas.size() + " folha(s) · " + lancamentos.size() + " lançamento(s)"

            );



            if (!folhas.isEmpty()) {

                doc.add(PdfStyledTable.sectionTitle("Resumo por barbeiro"));

                PdfPTable tabela = PdfStyledTable.create(5, 100);

                PdfStyledTable.addHeaderRow(tabela, "Barbeiro", "Mês", "Status", "Atend.", "Total R$");

                int i = 0;

                for (FolhaComissaoBarbeiro f : folhas) {

                    PdfStyledTable.addDataRow(

                            tabela,

                            i++,

                            nomeBarbeiroFolha(f),

                            f.getAnoMes() != null ? f.getAnoMes() : "-",

                            f.getStatus() != null ? f.getStatus().name() : "-",

                            String.valueOf(f.getQuantidadeAtendimentos()),

                            moeda(f.getValorTotal())

                    );

                }

                doc.add(tabela);

            }



            if (!lancamentos.isEmpty()) {

                doc.add(PdfStyledTable.sectionTitle("Lançamentos detalhados"));

                PdfPTable det = PdfStyledTable.create(5, 100);

                PdfStyledTable.addHeaderRow(det, "Data", "Barbeiro", "Serviço", "%", "Comissão R$");

                int i = 0;

                for (ComissaoLancamento l : lancamentos) {

                    PdfStyledTable.addDataRow(

                            det,

                            i++,

                            l.getCreatedAt() != null ? FMT.format(l.getCreatedAt()) : "-",

                            nomeBarbeiroLancamento(l),

                            nomeServicoLancamento(l),

                            percentual(l.getPercentualComissao()),

                            moeda(l.getValorComissao())

                    );

                }

                doc.add(det);

            }



            if (folhas.isEmpty() && lancamentos.isEmpty()) {

                doc.add(new Paragraph("Nenhum registro encontrado para os filtros selecionados.", PdfFonts.meta()));

            }



            doc.close();

            return baos.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException("Falha ao gerar PDF de comissões", e);

        }

    }



    public byte[] exportarRelatorioCompleto(RelatorioCompletoDTO r) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Document doc = PdfLetterheadEvent.newDocument();

            PdfWriter writer = PdfWriter.getInstance(doc, baos);

            writer.setPageEvent(new PdfLetterheadEvent("Relatório gerencial"));

            doc.open();



            PdfLetterheadEvent.addCoverBlock(

                    doc,

                    "Relatório completo",

                    "Período: " + r.inicio() + " a " + r.fim(),

                    PdfBrand.NAME

            );



            doc.add(PdfStyledTable.sectionTitle("Indicadores gerais"));

            PdfPTable geral = PdfStyledTable.create(2, 72);

            PdfStyledTable.addHeaderRow(geral, "Indicador", "Valor");

            int row = 0;

            row = addIndicator(geral, row, "Total agendamentos", String.valueOf(r.totalAgendamentos()));

            row = addIndicator(geral, row, "Cortes concluídos", String.valueOf(r.cortesConcluidos()));

            row = addIndicator(geral, row, "Cancelados", String.valueOf(r.cancelados()));

            row = addIndicator(geral, row, "Faltas (ausências)", String.valueOf(r.faltas()));

            row = addIndicator(geral, row, "Em andamento", String.valueOf(r.emAndamento()));

            row = addIndicator(geral, row, "Agendados/confirmados", String.valueOf(r.agendadosConfirmados()));

            row = addIndicator(geral, row, "Atrasos informados", String.valueOf(r.atrasosInformados()));

            row = addIndicator(geral, row, "Faturamento (concluídos)", moeda(r.faturamentoConcluidos()));

            row = addIndicator(geral, row, "Pagamentos pendentes", String.valueOf(r.pagamentosPendentes()));

            row = addIndicator(geral, row, "Pagamentos confirmados", String.valueOf(r.pagamentosPagos()));

            row = addIndicator(geral, row, "Comissões (total folhas)", moeda(r.totalComissoesPeriodo()));
            row = addIndicator(geral, row, "Comissões a pagar", moeda(r.comissaoAPagar()));
            row = addIndicator(geral, row, "Comissões em andamento", moeda(r.comissaoEmAndamento()));
            addIndicator(geral, row, "Comissões pagas", moeda(r.comissaoPago()));

            doc.add(geral);

            if (r.folhasComissao() != null && !r.folhasComissao().isEmpty()) {
                doc.add(PdfStyledTable.sectionTitle("Folhas de comissão"));
                PdfPTable folhas = PdfStyledTable.create(5, 100);
                PdfStyledTable.addHeaderRow(folhas, "Mês", "Barbeiro", "Status", "Atend.", "Valor R$");
                int fi = 0;
                for (var f : r.folhasComissao()) {
                    PdfStyledTable.addDataRow(
                            folhas,
                            fi++,
                            f.anoMes() != null ? f.anoMes() : "-",
                            f.barbeiroNome() != null ? f.barbeiroNome() : "-",
                            labelStatusFolha(f.status()),
                            String.valueOf(f.quantidadeAtendimentos()),
                            moeda(f.valorTotal()));
                }
                doc.add(folhas);
            }



            if (r.porStatus() != null && !r.porStatus().isEmpty()) {

                doc.add(PdfStyledTable.sectionTitle("Por status"));

                PdfPTable st = PdfStyledTable.create(2, 55);

                PdfStyledTable.addHeaderRow(st, "Status", "Quantidade");

                int i = 0;

                for (var s : r.porStatus()) {

                    PdfStyledTable.addDataRow(

                            st,

                            i++,

                            s.status() != null ? s.status().name() : "-",

                            String.valueOf(s.quantidade())

                    );

                }

                doc.add(st);

            }



            if (r.porServico() != null && !r.porServico().isEmpty()) {

                doc.add(PdfStyledTable.sectionTitle("Por serviço"));

                PdfPTable sv = PdfStyledTable.create(2, 70);

                PdfStyledTable.addHeaderRow(sv, "Serviço", "Quantidade");

                int i = 0;

                for (var s : r.porServico()) {

                    PdfStyledTable.addDataRow(

                            sv,

                            i++,

                            s.servicoNome() != null ? s.servicoNome() : "Serviço",

                            String.valueOf(s.quantidade())

                    );

                }

                doc.add(sv);

            }



            if (r.porBarbeiro() != null && !r.porBarbeiro().isEmpty()) {

                doc.add(PdfStyledTable.sectionTitle("Por barbeiro (detalhe)"));

                PdfPTable bb = PdfStyledTable.create(5, 100);

                PdfStyledTable.addHeaderRow(bb, "Barbeiro", "Total", "Concl.", "Canc.", "Faltas");

                int i = 0;

                for (var b : r.porBarbeiro()) {

                    PdfStyledTable.addDataRow(

                            bb,

                            i++,

                            b.barbeiroNome() != null ? b.barbeiroNome() : "Barbeiro",

                            String.valueOf(b.total()),

                            String.valueOf(b.concluidos()),

                            String.valueOf(b.cancelados()),

                            String.valueOf(b.faltas())

                    );

                }

                doc.add(bb);

            }



            doc.close();

            return baos.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException("Falha ao gerar PDF do relatório", e);

        }

    }



    private static String nomeBarbeiroFolha(FolhaComissaoBarbeiro f) {

        if (f.getBarbeiro() != null && f.getBarbeiro().getNome() != null) {

            return f.getBarbeiro().getNome();

        }

        return "Barbeiro";

    }



    private static String nomeBarbeiroLancamento(ComissaoLancamento l) {

        if (l.getBarbeiro() != null && l.getBarbeiro().getNome() != null) {

            return l.getBarbeiro().getNome();

        }

        return "Barbeiro";

    }



    private static String nomeServicoLancamento(ComissaoLancamento l) {

        Agendamento ag = l.getAgendamento();

        if (ag == null) {

            return "Atendimento";

        }

        if (ag.getItens() != null && !ag.getItens().isEmpty()) {

            String nomes = ag.getItens().stream()

                    .filter(item -> item.getServico() != null)

                    .map(item -> item.getServico().getNome())

                    .reduce((a, b) -> a + ", " + b)

                    .orElse(null);

            if (nomes != null && !nomes.isBlank()) {

                return nomes;

            }

        }

        if (ag.getServico() != null && ag.getServico().getNome() != null) {

            return ag.getServico().getNome();

        }

        return "Atendimento";

    }



    private static String percentual(BigDecimal p) {

        return p != null ? p.stripTrailingZeros().toPlainString() + "%" : "-";

    }



    private static int addIndicator(PdfPTable table, int row, String label, String value) {

        PdfStyledTable.addKeyValueRow(table, row, label, value);

        return row + 1;

    }



    private static String labelStatusFolha(com.example.barbearia.domain.StatusFolhaComissao status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case A_PAGAR -> "A pagar";
            case EM_ANDAMENTO -> "Em andamento";
            case PAGO -> "Pago";
        };
    }

    private static String moeda(BigDecimal v) {

        if (v == null) {

            return "R$ 0,00";

        }

        return String.format("R$ %.2f", v);

    }

}


