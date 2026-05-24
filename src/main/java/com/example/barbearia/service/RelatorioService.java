package com.example.barbearia.service;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.AgendamentoItem;
import com.example.barbearia.domain.FolhaComissaoBarbeiro;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.domain.StatusFolhaComissao;
import com.example.barbearia.domain.StatusPagamento;
import com.example.barbearia.dto.relatorio.ContagemPorStatusDTO;
import com.example.barbearia.dto.relatorio.RelatorioCompletoDTO;
import com.example.barbearia.dto.relatorio.RelatorioFaturamentoDTO;
import com.example.barbearia.dto.relatorio.RelatorioPorBarbeiroDTO;
import com.example.barbearia.dto.relatorio.RelatorioResumoDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.AgendamentoRepository;
import com.example.barbearia.repository.FolhaComissaoBarbeiroRepository;
import com.example.barbearia.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final AgendamentoRepository agendamentoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final FolhaComissaoBarbeiroRepository folhaComissaoRepository;
    private final BarbeiroService barbeiroService;
    private final PdfExportService pdfExportService;
    private final AgendamentoDetalheHelper agendamentoDetalheHelper;

    @Transactional(readOnly = true)
    public RelatorioResumoDTO resumo(LocalDate inicio, LocalDate fim) {
        ContextoRelatorio ctx = carregarContexto(inicio, fim);
        return new RelatorioResumoDTO(
                inicio,
                fim,
                ctx.total(),
                ctx.porStatus(),
                ctx.percentual(StatusAgendamento.CONCLUIDO),
                ctx.percentual(StatusAgendamento.CANCELADO),
                ctx.percentual(StatusAgendamento.FALTOU)
        );
    }

    @Transactional(readOnly = true)
    public RelatorioPorBarbeiroDTO porBarbeiro(LocalDate inicio, LocalDate fim) {
        ContextoRelatorio ctx = carregarContexto(inicio, fim);
        List<RelatorioPorBarbeiroDTO.ItemBarbeiroDTO> itens = ctx.porBarbeiro().entrySet().stream()
                .map(e -> new RelatorioPorBarbeiroDTO.ItemBarbeiroDTO(
                        e.getKey(),
                        e.getValue().nome(),
                        e.getValue().total()))
                .sorted(Comparator.comparing(RelatorioPorBarbeiroDTO.ItemBarbeiroDTO::quantidadeAgendamentos).reversed())
                .toList();
        return new RelatorioPorBarbeiroDTO(inicio, fim, itens);
    }

    @Transactional(readOnly = true)
    public RelatorioFaturamentoDTO faturamento(LocalDate inicio, LocalDate fim) {
        ContextoRelatorio ctx = carregarContexto(inicio, fim);
        return new RelatorioFaturamentoDTO(inicio, fim, ctx.faturamentoConcluidos());
    }

    @Transactional(readOnly = true)
    public RelatorioCompletoDTO completo(LocalDate inicio, LocalDate fim) {
        ContextoRelatorio ctx = carregarContexto(inicio, fim);
        return new RelatorioCompletoDTO(
                inicio,
                fim,
                ctx.total(),
                ctx.contagem(StatusAgendamento.CONCLUIDO),
                ctx.contagem(StatusAgendamento.CANCELADO),
                ctx.contagem(StatusAgendamento.FALTOU),
                ctx.contagem(StatusAgendamento.EM_ANDAMENTO),
                ctx.contagem(StatusAgendamento.AGENDADO) + ctx.contagem(StatusAgendamento.CONFIRMADO),
                ctx.atrasosInformados(),
                ctx.faturamentoConcluidos(),
                ctx.pagamentosPendentes(),
                ctx.pagamentosPagos(),
                ctx.comissoes().total(),
                ctx.comissoes().aPagar(),
                ctx.comissoes().emAndamento(),
                ctx.comissoes().pago(),
                ctx.comissoes().folhas(),
                ctx.porStatus(),
                ctx.itensServico(),
                ctx.porBarbeiroDetalhe(),
                ctx.itensBarbeiroStatus()
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportarCompletoPdf(LocalDate inicio, LocalDate fim) {
        return pdfExportService.exportarRelatorioCompleto(completo(inicio, fim));
    }

    private ContextoRelatorio carregarContexto(LocalDate inicio, LocalDate fim) {
        Periodo periodo = validarPeriodo(inicio, fim);
        ResumoComissao comissoes = resumirComissoes(inicio, fim);
        List<Agendamento> agendamentos = agendamentoRepository.findAgendamentosNoPeriodo(periodo.desde(), periodo.ate());
        agendamentos.forEach(agendamentoDetalheHelper::enriquecer);

        Map<StatusAgendamento, Long> porStatusMap = new EnumMap<>(StatusAgendamento.class);
        Map<UUID, BarbeiroAgg> porBarbeiro = new HashMap<>();
        Map<String, BarbeiroStatusAgg> barbeiroStatus = new HashMap<>();
        Map<UUID, ServicoAgg> porServico = new HashMap<>();
        BigDecimal faturamento = BigDecimal.ZERO;
        long atrasos = 0;

        for (Agendamento ag : agendamentos) {
            StatusAgendamento status = ag.getStatus() != null ? ag.getStatus() : StatusAgendamento.AGENDADO;
            porStatusMap.merge(status, 1L, Long::sum);

            if (status == StatusAgendamento.CONCLUIDO) {
                faturamento = faturamento.add(agendamentoDetalheHelper.valorTotal(ag));
            }
            if (ag.getAtrasoInformadoEm() != null) {
                atrasos++;
            }

            UUID barbeiroId = ag.getBarbeiroId();
            if (barbeiroId == null && ag.getBarbeiro() != null) {
                barbeiroId = ag.getBarbeiro().getId();
            }
            if (barbeiroId != null) {
                String nomeBarbeiro = ag.getBarbeiro() != null ? ag.getBarbeiro().getNome() : "Barbeiro";
                UUID idBarbeiro = barbeiroId;
                porBarbeiro.compute(idBarbeiro, (id, agg) -> {
                    if (agg == null) {
                        return new BarbeiroAgg(idBarbeiro, nomeBarbeiro, 1, status);
                    }
                    return agg.add(status);
                });
                String chaveStatus = idBarbeiro + "|" + status.name();
                barbeiroStatus.merge(
                        chaveStatus,
                        new BarbeiroStatusAgg(idBarbeiro, nomeBarbeiro, status, 1),
                        (a, b) -> new BarbeiroStatusAgg(idBarbeiro, nomeBarbeiro, status, a.quantidade() + 1)
                );
            }

            registrarServicosDoAgendamento(ag, porServico);
        }

        List<ContagemPorStatusDTO> porStatus = porStatusMap.entrySet().stream()
                .map(e -> new ContagemPorStatusDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ContagemPorStatusDTO::quantidade).reversed())
                .toList();

        List<RelatorioCompletoDTO.ItemBarbeiroDetalheDTO> porBarbeiroDetalhe = porBarbeiro.entrySet().stream()
                .map(e -> new RelatorioCompletoDTO.ItemBarbeiroDetalheDTO(
                        e.getKey(),
                        e.getValue().nome(),
                        e.getValue().total(),
                        e.getValue().concluidos(),
                        e.getValue().cancelados(),
                        e.getValue().faltas()))
                .sorted(Comparator.comparing(RelatorioCompletoDTO.ItemBarbeiroDetalheDTO::total).reversed())
                .toList();

        List<RelatorioCompletoDTO.ItemBarbeiroStatusDTO> itensBarbeiroStatus = barbeiroStatus.values().stream()
                .map(a -> new RelatorioCompletoDTO.ItemBarbeiroStatusDTO(
                        a.barbeiroId(), a.nome(), a.status(), a.quantidade()))
                .sorted(Comparator.comparing(RelatorioCompletoDTO.ItemBarbeiroStatusDTO::barbeiroNome))
                .toList();

        List<RelatorioCompletoDTO.ItemServicoDTO> itensServico = porServico.entrySet().stream()
                .map(e -> new RelatorioCompletoDTO.ItemServicoDTO(e.getKey(), e.getValue().nome(), e.getValue().quantidade()))
                .sorted(Comparator.comparingLong(RelatorioCompletoDTO.ItemServicoDTO::quantidade).reversed())
                .toList();

        return new ContextoRelatorio(
                agendamentos.size(),
                porStatus,
                porStatusMap,
                porBarbeiro,
                porBarbeiroDetalhe,
                itensBarbeiroStatus,
                itensServico,
                faturamento.setScale(2, RoundingMode.HALF_UP),
                atrasos,
                contarPagamentos(periodo, StatusPagamento.PENDENTE),
                contarPagamentos(periodo, StatusPagamento.PAGO),
                comissoes
        );
    }

    private ResumoComissao resumirComissoes(LocalDate inicio, LocalDate fim) {
        List<String> meses = mesesNoPeriodo(inicio, fim);
        if (meses.isEmpty()) {
            return ResumoComissao.vazio();
        }
        List<FolhaComissaoBarbeiro> folhas = folhaComissaoRepository.findByAnoMesIn(meses);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal aPagar = BigDecimal.ZERO;
        BigDecimal emAndamento = BigDecimal.ZERO;
        BigDecimal pago = BigDecimal.ZERO;
        List<RelatorioCompletoDTO.ItemFolhaComissaoDTO> itens = new ArrayList<>();

        for (FolhaComissaoBarbeiro folha : folhas) {
            enriquecerFolha(folha);
            BigDecimal valor = folha.getValorTotal() != null ? folha.getValorTotal() : BigDecimal.ZERO;
            StatusFolhaComissao status = folha.getStatus() != null ? folha.getStatus() : StatusFolhaComissao.A_PAGAR;
            total = total.add(valor);
            switch (status) {
                case A_PAGAR -> aPagar = aPagar.add(valor);
                case EM_ANDAMENTO -> emAndamento = emAndamento.add(valor);
                case PAGO -> pago = pago.add(valor);
            }
            var barbeiro = folha.getBarbeiro();
            itens.add(new RelatorioCompletoDTO.ItemFolhaComissaoDTO(
                    folha.getId(),
                    barbeiro != null ? barbeiro.getId() : folha.getBarbeiroId(),
                    barbeiro != null && barbeiro.getNome() != null ? barbeiro.getNome() : "Barbeiro",
                    folha.getAnoMes(),
                    status,
                    valor.setScale(2, RoundingMode.HALF_UP),
                    folha.getQuantidadeAtendimentos(),
                    folha.getPagoEm()
            ));
        }

        itens.sort(Comparator
                .comparing(RelatorioCompletoDTO.ItemFolhaComissaoDTO::anoMes, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RelatorioCompletoDTO.ItemFolhaComissaoDTO::barbeiroNome, Comparator.nullsLast(String::compareToIgnoreCase)));

        return new ResumoComissao(
                total.setScale(2, RoundingMode.HALF_UP),
                aPagar.setScale(2, RoundingMode.HALF_UP),
                emAndamento.setScale(2, RoundingMode.HALF_UP),
                pago.setScale(2, RoundingMode.HALF_UP),
                itens
        );
    }

    private void enriquecerFolha(FolhaComissaoBarbeiro folha) {
        if (folha.getBarbeiro() == null && folha.getBarbeiroId() != null) {
            folha.setBarbeiro(barbeiroService.findById(folha.getBarbeiroId()));
        }
    }

    private static List<String> mesesNoPeriodo(LocalDate inicio, LocalDate fim) {
        YearMonth inicioMes = YearMonth.from(inicio);
        YearMonth fimMes = YearMonth.from(fim);
        List<String> meses = new ArrayList<>();
        for (YearMonth ym = inicioMes; !ym.isAfter(fimMes); ym = ym.plusMonths(1)) {
            meses.add(String.format("%04d-%02d", ym.getYear(), ym.getMonthValue()));
        }
        return meses;
    }

    private void registrarServicosDoAgendamento(Agendamento ag, Map<UUID, ServicoAgg> porServico) {
        if (ag.getItens() != null && !ag.getItens().isEmpty()) {
            for (AgendamentoItem item : ag.getItens()) {
                UUID servicoId = item.getServicoId();
                if (servicoId == null && item.getServico() != null) {
                    servicoId = item.getServico().getId();
                }
                if (servicoId == null) {
                    continue;
                }
                String nome = item.getServico() != null && item.getServico().getNome() != null
                        ? item.getServico().getNome()
                        : "Serviço";
                porServico.merge(servicoId, new ServicoAgg(nome, 1),
                        (a, b) -> new ServicoAgg(a.nome(), a.quantidade() + 1));
            }
            return;
        }
        UUID servicoId = ag.getServicoId();
        if (servicoId == null && ag.getServico() != null) {
            servicoId = ag.getServico().getId();
        }
        if (servicoId == null) {
            return;
        }
        String nome = ag.getServico() != null && ag.getServico().getNome() != null
                ? ag.getServico().getNome()
                : "Serviço";
        porServico.merge(servicoId, new ServicoAgg(nome, 1),
                (a, b) -> new ServicoAgg(a.nome(), a.quantidade() + 1));
    }

    private long contarPagamentos(Periodo periodo, StatusPagamento status) {
        try {
            return pagamentoRepository.countByAgendamentoInicioBetweenAndStatus(
                    periodo.desde(), periodo.ate(), status);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static Periodo validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new ApiException("Parâmetros inicio e fim são obrigatórios (formato yyyy-MM-dd)", HttpStatus.BAD_REQUEST);
        }
        if (fim.isBefore(inicio)) {
            throw new ApiException("A data fim não pode ser anterior à data inicio", HttpStatus.BAD_REQUEST);
        }
        return new Periodo(inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
    }

    private record Periodo(LocalDateTime desde, LocalDateTime ate) {
    }

    private record ServicoAgg(String nome, long quantidade) {
    }

    private record BarbeiroAgg(UUID barbeiroId, String nome, long total, long concluidos, long cancelados, long faltas) {
        BarbeiroAgg(UUID barbeiroId, String nome, long count, StatusAgendamento status) {
            this(barbeiroId, nome, count,
                    status == StatusAgendamento.CONCLUIDO ? count : 0,
                    status == StatusAgendamento.CANCELADO ? count : 0,
                    status == StatusAgendamento.FALTOU ? count : 0);
        }

        BarbeiroAgg add(StatusAgendamento status) {
            return new BarbeiroAgg(
                    barbeiroId,
                    nome,
                    total + 1,
                    concluidos + (status == StatusAgendamento.CONCLUIDO ? 1 : 0),
                    cancelados + (status == StatusAgendamento.CANCELADO ? 1 : 0),
                    faltas + (status == StatusAgendamento.FALTOU ? 1 : 0)
            );
        }
    }

    private record BarbeiroStatusAgg(UUID barbeiroId, String nome, StatusAgendamento status, long quantidade) {
    }

    private record ContextoRelatorio(
            long total,
            List<ContagemPorStatusDTO> porStatus,
            Map<StatusAgendamento, Long> porStatusMap,
            Map<UUID, BarbeiroAgg> porBarbeiro,
            List<RelatorioCompletoDTO.ItemBarbeiroDetalheDTO> porBarbeiroDetalhe,
            List<RelatorioCompletoDTO.ItemBarbeiroStatusDTO> itensBarbeiroStatus,
            List<RelatorioCompletoDTO.ItemServicoDTO> itensServico,
            BigDecimal faturamentoConcluidos,
            long atrasosInformados,
            long pagamentosPendentes,
            long pagamentosPagos,
            ResumoComissao comissoes
    ) {
        long contagem(StatusAgendamento status) {
            return porStatusMap.getOrDefault(status, 0L);
        }

        BigDecimal percentual(StatusAgendamento status) {
            long parte = contagem(status);
            if (total <= 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(parte)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }
    }

    private record ResumoComissao(
            BigDecimal total,
            BigDecimal aPagar,
            BigDecimal emAndamento,
            BigDecimal pago,
            List<RelatorioCompletoDTO.ItemFolhaComissaoDTO> folhas
    ) {
        static ResumoComissao vazio() {
            BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new ResumoComissao(zero, zero, zero, zero, List.of());
        }
    }
}
