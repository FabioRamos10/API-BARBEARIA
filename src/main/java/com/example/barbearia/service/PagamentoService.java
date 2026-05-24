package com.example.barbearia.service;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.FormaPagamento;
import com.example.barbearia.domain.Pagamento;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.domain.StatusPagamento;
import com.example.barbearia.dto.PagamentoCreateDTO;
import com.example.barbearia.dto.PagamentoResponseDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.mail.PixPayloadService;
import com.example.barbearia.repository.AgendamentoRepository;
import com.example.barbearia.repository.PagamentoRepository;
import com.example.barbearia.security.AuthzHelper;
import com.example.barbearia.storage.ArquivoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PixPayloadService pixPayloadService;
    private final ArquivoStorageService arquivoStorageService;
    private final NotificacaoService notificacaoService;
    private final ComissaoService comissaoService;
    private final ClienteService clienteService;
    private final AgendamentoDetalheHelper agendamentoDetalheHelper;

    @Transactional
    public void processarAoConcluir(UUID agendamentoId) {
        if (pagamentoRepository.findByAgendamento_Id(agendamentoId).isPresent()) {
            comissaoService.registrarComissaoAoConcluir(agendamentoId);
            return;
        }

        Agendamento agendamento = agendamentoRepository.findByIdComDetalhes(agendamentoId)
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));

        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) {
            return;
        }

        BigDecimal valor = agendamentoDetalheHelper.valorTotal(agendamento);
        var pix = pixPayloadService.gerar(valor, agendamentoId);

        Pagamento pagamento = Pagamento.builder()
                .agendamento(agendamento)
                .formaPagamento(FormaPagamento.PIX)
                .valor(valor)
                .status(StatusPagamento.PENDENTE)
                .pixCopiaCola(pix.copiaCola())
                .pixQrCodeUrl(pix.qrCodeUrl())
                .build();

        pagamentoRepository.save(pagamento);
        comissaoService.registrarComissaoAoConcluir(agendamentoId);

        try {
            notificacaoService.notificarPagamentoPixCliente(agendamentoId, valor, pix.copiaCola(), pix.qrCodeUrl());
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public PagamentoResponseDTO criar(PagamentoCreateDTO dto, Authentication authentication) {
        if (!podeGerenciarPagamento(authentication)) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }

        Agendamento agendamento = agendamentoRepository.findByIdComDetalhes(dto.agendamentoId())
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));

        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO
                && agendamento.getStatus() != StatusAgendamento.EM_ANDAMENTO) {
            throw new ApiException(
                    "Pagamento só pode ser registrado para atendimentos em andamento ou concluídos",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (pagamentoRepository.findByAgendamento_Id(agendamento.getId()).isPresent()) {
            throw new ApiException("Já existe pagamento para este agendamento", HttpStatus.CONFLICT);
        }

        BigDecimal valor = dto.valor() != null ? dto.valor() : agendamentoDetalheHelper.valorTotal(agendamento);
        boolean pagoImediato = dto.formaPagamento() != FormaPagamento.PIX
                || Boolean.TRUE.equals(dto.confirmarImediato());

        Pagamento pagamento = Pagamento.builder()
                .agendamento(agendamento)
                .formaPagamento(dto.formaPagamento())
                .valor(valor)
                .status(pagoImediato ? StatusPagamento.PAGO : StatusPagamento.PENDENTE)
                .build();

        if (dto.formaPagamento() == FormaPagamento.PIX && !pagoImediato) {
            var pix = pixPayloadService.gerar(valor, agendamento.getId());
            pagamento.setPixCopiaCola(pix.copiaCola());
            pagamento.setPixQrCodeUrl(pix.qrCodeUrl());
        }

        if (pagoImediato) {
            pagamento.setDataPagamento(LocalDateTime.now());
            pagamento.setObservacao(dto.observacao());
        }

        pagamento = pagamentoRepository.save(pagamento);

        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            comissaoService.registrarComissaoAoConcluir(agendamento.getId());
        }

        return toResponse(pagamento);
    }

    @Transactional(readOnly = true)
    public PagamentoResponseDTO buscarPorAgendamento(UUID agendamentoId, Authentication authentication) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));
        validarLeituraAgendamento(agendamento, authentication);

        Pagamento pagamento = pagamentoRepository.findByAgendamento_Id(agendamentoId)
                .orElseThrow(() -> new ApiException("Pagamento não encontrado", HttpStatus.NOT_FOUND));
        if (pagamento.getAgendamento() == null) {
            pagamento.setAgendamento(agendamento);
        }

        return toResponse(pagamento);
    }

    @Transactional
    public PagamentoResponseDTO enviarComprovante(UUID pagamentoId, MultipartFile arquivo, Authentication authentication) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new ApiException("Pagamento não encontrado", HttpStatus.NOT_FOUND));

        validarClienteDono(pagamento.getAgendamento(), authentication);

        if (pagamento.getFormaPagamento() != FormaPagamento.PIX) {
            throw new ApiException("Comprovante só se aplica a pagamentos PIX", HttpStatus.BAD_REQUEST);
        }
        if (pagamento.getStatus() == StatusPagamento.PAGO) {
            throw new ApiException("Pagamento já confirmado", HttpStatus.BAD_REQUEST);
        }

        var salvo = arquivoStorageService.salvarComprovante(pagamentoId, arquivo);
        pagamento.setComprovantePath(salvo.storagePath());
        pagamento.setComprovanteEnviadoEm(LocalDateTime.now());
        pagamento = pagamentoRepository.save(pagamento);

        try {
            notificacaoService.notificarComprovantePixEnviado(pagamento.getAgendamento().getId());
        } catch (Exception ignored) {
        }

        return toResponse(pagamento);
    }

    @Transactional
    public PagamentoResponseDTO confirmarPagamento(UUID id, Authentication authentication) {
        if (!podeGerenciarPagamento(authentication)) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }

        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ApiException("Pagamento não encontrado", HttpStatus.NOT_FOUND));

        if (pagamento.getStatus() == StatusPagamento.PAGO) {
            throw new ApiException("Pagamento já confirmado", HttpStatus.BAD_REQUEST);
        }
        if (pagamento.getStatus() == StatusPagamento.CANCELADO) {
            throw new ApiException("Pagamento cancelado não pode ser confirmado", HttpStatus.BAD_REQUEST);
        }

        pagamento.setStatus(StatusPagamento.PAGO);
        pagamento.setDataPagamento(LocalDateTime.now());
        return toResponse(pagamentoRepository.save(pagamento));
    }

    @Transactional(readOnly = true)
    public Resource baixarComprovante(UUID pagamentoId, Authentication authentication) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new ApiException("Pagamento não encontrado", HttpStatus.NOT_FOUND));

        Agendamento agendamento = pagamento.getAgendamento();
        if (AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            validarClienteDono(agendamento, authentication);
        } else if (!podeGerenciarPagamento(authentication)) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }

        Path path = arquivoStorageService.resolver(pagamento.getComprovantePath());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException("Comprovante não encontrado", HttpStatus.NOT_FOUND);
            }
            return resource;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException("Falha ao ler comprovante", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private PagamentoResponseDTO toResponse(Pagamento pagamento) {
        String chave = pagamento.getPixCopiaCola() != null ? pixPayloadService.getChaveExibicao() : null;
        return PagamentoResponseDTO.from(pagamento, chave);
    }

    private static boolean podeGerenciarPagamento(Authentication authentication) {
        return AuthzHelper.isStaff(authentication)
                || AuthzHelper.hasRole(authentication, "ROLE_BARBEIRO");
    }

    private void validarClienteDono(Agendamento agendamento, Authentication authentication) {
        if (!AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            throw new ApiException("Somente o cliente pode enviar o comprovante", HttpStatus.FORBIDDEN);
        }
        var cliente = clienteService.findEntityByUserEmail(AuthzHelper.email(authentication));
        UUID donoId = AuthzHelper.clienteIdOf(agendamento);
        if (donoId == null || !donoId.equals(cliente.getId())) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
    }

    private void validarLeituraAgendamento(Agendamento agendamento, Authentication authentication) {
        if (AuthzHelper.isStaff(authentication) || AuthzHelper.hasRole(authentication, "ROLE_BARBEIRO")) {
            return;
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            var cliente = clienteService.findEntityByUserEmail(AuthzHelper.email(authentication));
            UUID donoId = AuthzHelper.clienteIdOf(agendamento);
            if (donoId != null && donoId.equals(cliente.getId())) {
                return;
            }
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }
}
