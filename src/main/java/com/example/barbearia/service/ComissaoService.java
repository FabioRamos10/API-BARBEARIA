package com.example.barbearia.service;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.ComissaoLancamento;
import com.example.barbearia.domain.FolhaComissaoBarbeiro;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.domain.StatusFolhaComissao;
import com.example.barbearia.dto.comissao.FolhaComissaoResponseDTO;
import com.example.barbearia.dto.comissao.FolhaComissaoStatusDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.AgendamentoRepository;
import com.example.barbearia.repository.ComissaoLancamentoRepository;
import com.example.barbearia.repository.FolhaComissaoBarbeiroRepository;
import com.example.barbearia.security.AuthzHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComissaoService {

    private final ComissaoLancamentoRepository lancamentoRepository;
    private final FolhaComissaoBarbeiroRepository folhaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroService barbeiroService;
    private final AgendamentoDetalheHelper agendamentoDetalheHelper;
    private final PdfExportService pdfExportService;

    @Transactional
    public void registrarComissaoAoConcluir(UUID agendamentoId) {
        Agendamento ag = agendamentoRepository.findByIdComDetalhes(agendamentoId).orElse(null);
        if (ag == null || ag.getStatus() != StatusAgendamento.CONCLUIDO) {
            return;
        }
        if (lancamentoRepository.findByAgendamento_Id(agendamentoId).isPresent()) {
            return;
        }

        Barbeiro barbeiro = ag.getBarbeiro();
        if (barbeiro == null && ag.getBarbeiroId() != null) {
            barbeiro = barbeiroService.findById(ag.getBarbeiroId());
        }
        if (barbeiro == null) {
            return;
        }
        BigDecimal valorServico = ag.valorTotalServicos();
        BigDecimal percentual = barbeiro.getPercentualComissao() != null
                ? barbeiro.getPercentualComissao()
                : BigDecimal.valueOf(40);
        BigDecimal valorComissao = valorServico.multiply(percentual)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        String anoMes = YearMonth.from(ag.getInicio()).toString();

        lancamentoRepository.save(ComissaoLancamento.builder()
                .agendamento(ag)
                .barbeiro(barbeiro)
                .anoMes(anoMes)
                .valorServico(valorServico)
                .percentualComissao(percentual)
                .valorComissao(valorComissao)
                .build());

        recalcularFolha(barbeiro.getId(), anoMes);
    }

    @Transactional(readOnly = true)
    public List<FolhaComissaoResponseDTO> listarFolhas(String anoMes, UUID barbeiroId, Authentication auth) {
        UUID barbeiroConsulta = resolverBarbeiroConsulta(auth, barbeiroId);
        validarLeituraComissao(auth, barbeiroConsulta);

        List<FolhaComissaoBarbeiro> folhas;
        if (barbeiroConsulta != null && anoMes != null && !anoMes.isBlank()) {
            folhas = folhaRepository.findByBarbeiro_IdAndAnoMes(barbeiroConsulta, anoMes)
                    .map(List::of)
                    .orElse(List.of());
        } else if (barbeiroConsulta != null) {
            folhas = folhaRepository.findByBarbeiro_IdOrderByAnoMesDesc(barbeiroConsulta);
        } else if (anoMes != null && !anoMes.isBlank()) {
            folhas = folhaRepository.findByAnoMes(anoMes);
        } else if (AuthzHelper.hasRole(auth, "ROLE_ADMIN") || AuthzHelper.hasRole(auth, "ROLE_RECEPCIONISTA")) {
            folhas = folhaRepository.findAll();
            folhas.forEach(this::enriquecerFolha);
            folhas = folhas.stream()
                    .sorted(java.util.Comparator
                            .comparing(FolhaComissaoBarbeiro::getAnoMes, java.util.Comparator.reverseOrder())
                            .thenComparing(ComissaoService::nomeBarbeiro, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } else {
            throw new ApiException("Informe o mês (yyyy-MM) ou barbeiroId", HttpStatus.BAD_REQUEST);
        }

        return folhas.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarMesesRegistrados(Authentication auth) {
        if (!AuthzHelper.hasRole(auth, "ROLE_ADMIN") && !AuthzHelper.hasRole(auth, "ROLE_RECEPCIONISTA")) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        return folhaRepository.findDistinctAnoMesOrderByAnoMesDesc();
    }

    @Transactional
    public FolhaComissaoResponseDTO atualizarStatusFolha(UUID folhaId, FolhaComissaoStatusDTO dto, Authentication auth) {
        if (!AuthzHelper.hasRole(auth, "ROLE_ADMIN")) {
            throw new ApiException("Apenas administrador pode alterar status da folha", HttpStatus.FORBIDDEN);
        }

        FolhaComissaoBarbeiro folha = folhaRepository.findById(folhaId)
                .orElseThrow(() -> new ApiException("Folha de comissão não encontrada", HttpStatus.NOT_FOUND));

        folha.setStatus(dto.status());
        if (dto.status() == StatusFolhaComissao.PAGO) {
            folha.setPagoEm(LocalDateTime.now());
        } else {
            folha.setPagoEm(null);
        }
        enriquecerFolha(folha);
        return FolhaComissaoResponseDTO.from(folhaRepository.save(folha));
    }

    private UUID resolverBarbeiroConsulta(Authentication auth, UUID barbeiroId) {
        if (AuthzHelper.hasRole(auth, "ROLE_BARBEIRO")) {
            return barbeiroService.findEntityByUserEmail(AuthzHelper.email(auth)).getId();
        }
        return barbeiroId;
    }

    private FolhaComissaoResponseDTO toDto(FolhaComissaoBarbeiro folha) {
        enriquecerFolha(folha);
        return FolhaComissaoResponseDTO.from(folha);
    }

    private void enriquecerFolha(FolhaComissaoBarbeiro folha) {
        if (folha.getBarbeiro() == null && folha.getBarbeiroId() != null) {
            folha.setBarbeiro(barbeiroService.findById(folha.getBarbeiroId()));
        }
    }

    private void enriquecerLancamento(ComissaoLancamento lancamento) {
        if (lancamento.getBarbeiro() == null && lancamento.getBarbeiroId() != null) {
            lancamento.setBarbeiro(barbeiroService.findById(lancamento.getBarbeiroId()));
        }
        if (lancamento.getAgendamento() == null && lancamento.getAgendamentoId() != null) {
            agendamentoRepository.findByIdComDetalhes(lancamento.getAgendamentoId())
                    .ifPresent(lancamento::setAgendamento);
        } else if (lancamento.getAgendamento() != null) {
            agendamentoDetalheHelper.enriquecer(lancamento.getAgendamento());
        }
    }

    private static String nomeBarbeiro(FolhaComissaoBarbeiro folha) {
        if (folha.getBarbeiro() != null && folha.getBarbeiro().getNome() != null) {
            return folha.getBarbeiro().getNome();
        }
        return "";
    }

    private void recalcularFolha(UUID barbeiroId, String anoMes) {
        List<ComissaoLancamento> lancamentos = lancamentoRepository
                .findByBarbeiro_IdAndAnoMesOrderByCreatedAtDesc(barbeiroId, anoMes);
        BigDecimal total = lancamentos.stream()
                .map(ComissaoLancamento::getValorComissao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FolhaComissaoBarbeiro folha = folhaRepository.findByBarbeiro_IdAndAnoMes(barbeiroId, anoMes)
                .orElseGet(() -> FolhaComissaoBarbeiro.builder()
                        .barbeiro(barbeiroService.findById(barbeiroId))
                        .anoMes(anoMes)
                        .status(StatusFolhaComissao.A_PAGAR)
                        .build());

        folha.setValorTotal(total);
        folha.setQuantidadeAtendimentos(lancamentos.size());
        folhaRepository.save(folha);
    }

    @Transactional(readOnly = true)
    public byte[] gerarPdf(String anoMes, UUID barbeiroId, boolean todos, Authentication auth) {
        UUID barbeiroConsulta = resolverBarbeiroConsulta(auth, barbeiroId);
        validarLeituraComissao(auth, barbeiroConsulta);

        List<FolhaComissaoBarbeiro> folhas;
        List<ComissaoLancamento> lancamentos;
        String titulo;

        if (barbeiroConsulta != null) {
            var barbeiro = barbeiroService.findById(barbeiroConsulta);
            if (anoMes != null && !anoMes.isBlank()) {
                folhas = folhaRepository.findByBarbeiro_IdAndAnoMes(barbeiroConsulta, anoMes)
                        .map(List::of)
                        .orElse(List.of());
                lancamentos = lancamentoRepository.findByBarbeiroAndAnoMesComDetalhes(barbeiroConsulta, anoMes);
                titulo = "Comissões — " + barbeiro.getNome() + " — " + anoMes;
            } else {
                folhas = folhaRepository.findByBarbeiro_IdOrderByAnoMesDesc(barbeiroConsulta);
                lancamentos = folhas.stream()
                        .flatMap(f -> lancamentoRepository
                                .findByBarbeiroAndAnoMesComDetalhes(barbeiroConsulta, f.getAnoMes()).stream())
                        .toList();
                titulo = "Comissões — " + barbeiro.getNome() + " — todos os meses";
            }
        } else if (anoMes != null && !anoMes.isBlank()) {
            folhas = folhaRepository.findByAnoMes(anoMes);
            lancamentos = lancamentoRepository.findByAnoMesComDetalhes(anoMes);
            titulo = "Comissões — " + anoMes;
        } else if (todos) {
            folhas = folhaRepository.findAll();
            lancamentos = folhas.stream()
                    .filter(f -> f.getBarbeiroId() != null && f.getAnoMes() != null)
                    .flatMap(f -> lancamentoRepository
                            .findByBarbeiroAndAnoMesComDetalhes(f.getBarbeiroId(), f.getAnoMes())
                            .stream())
                    .toList();
            titulo = "Comissões — histórico completo";
        } else {
            throw new ApiException("Informe anoMes, barbeiroId ou todos=true", HttpStatus.BAD_REQUEST);
        }

        folhas.forEach(this::enriquecerFolha);
        lancamentos.forEach(this::enriquecerLancamento);
        return pdfExportService.exportarComissoes(titulo, folhas, lancamentos);
    }

    private void validarLeituraComissao(Authentication auth, UUID barbeiroId) {
        if (AuthzHelper.hasRole(auth, "ROLE_ADMIN") || AuthzHelper.hasRole(auth, "ROLE_RECEPCIONISTA")) {
            return;
        }
        if (AuthzHelper.hasRole(auth, "ROLE_BARBEIRO")) {
            var me = barbeiroService.findEntityByUserEmail(AuthzHelper.email(auth));
            if (barbeiroId == null || me.getId().equals(barbeiroId)) {
                return;
            }
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }
}
