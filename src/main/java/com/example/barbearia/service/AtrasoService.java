package com.example.barbearia.service;

import com.example.barbearia.domain.*;
import com.example.barbearia.dto.AtrasoMensagemCreateDTO;
import com.example.barbearia.dto.AtrasoMensagemResponseDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.AgendamentoRepository;
import com.example.barbearia.repository.AtrasoMensagemRepository;
import com.example.barbearia.security.AuthzHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AtrasoService {

    private static final int MINUTOS_REAJUSTE_PADRAO = 15;

    private final AgendamentoRepository agendamentoRepository;
    private final AtrasoMensagemRepository atrasoMensagemRepository;
    private final UserService userService;
    private final ClienteService clienteService;
    private final BarbeiroService barbeiroService;
    private final AlertaService alertaService;
    private final NotificacaoService notificacaoService;
    private final AgendamentoDetalheHelper agendamentoDetalheHelper;

    @Transactional
    public void aposInformarAtraso(Agendamento agendamento, int minutos, String motivo) {
        agendamentoDetalheHelper.enriquecer(agendamento);
        agendamento.setAtrasoStatus(StatusAtraso.INFORMADO);
        String nomeCliente = agendamento.getCliente() != null ? agendamento.getCliente().getNome() : "Cliente";
        String resumo = nomeCliente + " avisou " + minutos + " min. Motivo: " + motivo;
        alertaService.notificarGestao(agendamento.getId(), "Cliente informou atraso", resumo);
        User barbeiroUser = agendamento.getBarbeiro() != null ? agendamento.getBarbeiro().getUser() : null;
        if (barbeiroUser != null) {
            alertaService.notificarUsuario(
                    barbeiroUser.getId(),
                    agendamento.getId(),
                    "Atraso na sua agenda",
                    resumo,
                    TipoAlerta.ATRASO
            );
        }
    }

    @Transactional
    public Agendamento confirmarAtraso(UUID agendamentoId, Authentication auth) {
        if (!AuthzHelper.isStaff(auth) && !AuthzHelper.hasRole(auth, "ROLE_BARBEIRO")) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }

        Agendamento ag = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));
        agendamentoDetalheHelper.enriquecer(ag);

        if (ag.getAtrasoInformadoEm() == null) {
            throw new ApiException("Nenhum atraso informado para este agendamento", HttpStatus.BAD_REQUEST);
        }
        if (ag.getAtrasoStatus() == StatusAtraso.CONFIRMADO) {
            throw new ApiException("Atraso já confirmado", HttpStatus.BAD_REQUEST);
        }

        validarAcessoEquipe(ag, auth);

        int minutosReajuste = resolverMinutosReajuste(ag);

        LocalDateTime inicioOriginal = ag.getInicio();
        ag.setInicio(ag.getInicio().plusMinutes(minutosReajuste));
        ag.setFim(ag.getFim().plusMinutes(minutosReajuste));
        ag.setAtrasoStatus(StatusAtraso.CONFIRMADO);
        ag.setAtrasoConfirmadoEm(LocalDateTime.now());
        agendamentoRepository.save(ag);

        List<StatusAgendamento> statuses = Arrays.asList(
                StatusAgendamento.AGENDADO,
                StatusAgendamento.CONFIRMADO,
                StatusAgendamento.EM_ANDAMENTO
        );
        UUID barbeiroId = AuthzHelper.barbeiroIdOf(ag);
        if (barbeiroId == null) {
            throw new ApiException("Barbeiro do agendamento não encontrado", HttpStatus.BAD_REQUEST);
        }
        List<Agendamento> seguintes = agendamentoRepository.findSubsequentesBarbeiro(
                barbeiroId,
                inicioOriginal,
                statuses,
                ag.getId()
        );
        String nomeClienteAtraso = agendamentoDetalheHelper.nomeCliente(ag);
        for (Agendamento prox : seguintes) {
            prox.setInicio(prox.getInicio().plusMinutes(minutosReajuste));
            prox.setFim(prox.getFim().plusMinutes(minutosReajuste));
            agendamentoRepository.save(prox);
            notificarReagendamentoPorAtraso(ag, prox, minutosReajuste, nomeClienteAtraso);
        }

        User staff = userService.requireByEmail(AuthzHelper.email(auth));
        String textoOk = "Atraso confirmado. Seu horário foi reajustado em +" + minutosReajuste
                + " minuto" + (minutosReajuste == 1 ? "" : "s") + " para organizar a fila. Novo horário: "
                + ag.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ".";
        salvarMensagemInterna(ag, staff, textoOk, true);

        if (ag.getCliente().getUser() != null) {
            alertaService.notificarUsuario(
                    ag.getCliente().getUser().getId(),
                    ag.getId(),
                    "Atraso confirmado",
                    textoOk,
                    TipoAlerta.ATRASO
            );
        }

        try {
            notificacaoService.notificarRespostaAtraso(ag.getId(), textoOk);
        } catch (Exception ignored) {
        }

        return ag;
    }

    @Transactional(readOnly = true)
    public List<AtrasoMensagemResponseDTO> listarMensagens(UUID agendamentoId, Authentication auth) {
        Agendamento ag = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));
        validarLeituraAtraso(ag, auth);
        return atrasoMensagemRepository.findByAgendamento_IdOrderByCreatedAtAsc(agendamentoId).stream()
                .map(AtrasoMensagemResponseDTO::from)
                .toList();
    }

    @Transactional
    public AtrasoMensagemResponseDTO enviarMensagem(
            UUID agendamentoId,
            AtrasoMensagemCreateDTO dto,
            Authentication auth
    ) {
        Agendamento ag = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));
        validarLeituraAtraso(ag, auth);

        if (ag.getAtrasoInformadoEm() == null) {
            throw new ApiException("Informe o atraso antes de enviar mensagens", HttpStatus.BAD_REQUEST);
        }

        User autor = userService.requireByEmail(AuthzHelper.email(auth));
        AtrasoMensagem msg = salvarMensagemInterna(ag, autor, dto.texto().trim(), AuthzHelper.isStaff(auth)
                || AuthzHelper.hasRole(auth, "ROLE_BARBEIRO"));

        agendamentoDetalheHelper.enriquecer(ag);
        notificarParticipantesMensagemAtraso(ag, auth, autor, dto.texto().trim());

        return AtrasoMensagemResponseDTO.from(msg);
    }

    private void notificarReagendamentoPorAtraso(
            Agendamento agendamentoAtraso,
            Agendamento prox,
            int minutosReajuste,
            String nomeClienteAtraso
    ) {
        try {
            agendamentoDetalheHelper.enriquecer(prox);
            String horario = prox.getInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String msg = "Ajuste na agenda: outro cliente (" + nomeClienteAtraso + ") teve atraso de "
                    + minutosReajuste + " min confirmado. Seu atendimento foi movido para " + horario
                    + " (+" + minutosReajuste + " min).";
            if (prox.getCliente() != null && prox.getCliente().getUser() != null) {
                alertaService.notificarUsuario(
                        prox.getCliente().getUser().getId(),
                        prox.getId(),
                        "Horário reajustado",
                        msg,
                        TipoAlerta.ATRASO
                );
            }
            notificacaoService.notificarReagendamentoAtraso(
                    prox.getId(),
                    minutosReajuste,
                    horario,
                    nomeClienteAtraso
            );
        } catch (Exception ignored) {
        }
    }

    private static int resolverMinutosReajuste(Agendamento ag) {
        Integer informado = ag.getAtrasoMinutos();
        if (informado != null && informado > 0) {
            return informado;
        }
        return MINUTOS_REAJUSTE_PADRAO;
    }

    private AtrasoMensagem salvarMensagemInterna(Agendamento ag, User autor, String texto, boolean oficial) {
        return atrasoMensagemRepository.save(AtrasoMensagem.builder()
                .agendamentoId(ag.getId())
                .agendamento(ag)
                .autorUserId(autor.getId())
                .autorNome(autor.getNome())
                .autorRole(autor.getRole())
                .texto(texto)
                .respostaOficial(oficial)
                .build());
    }

    private void notificarParticipantesMensagemAtraso(
            Agendamento ag,
            Authentication auth,
            User autor,
            String texto
    ) {
        try {
            notificacaoService.notificarMensagemAtraso(ag.getId(), autor.getNome(), texto);
        } catch (Exception ignored) {
        }

        try {
            String nomeCliente = ag.getCliente() != null ? ag.getCliente().getNome() : "Cliente";
            if (AuthzHelper.hasRole(auth, "ROLE_CLIENTE")) {
                String resumo = nomeCliente + ": " + texto;
                alertaService.notificarGestao(ag.getId(), "Nova mensagem sobre atraso", resumo);
                if (ag.getBarbeiro() != null && ag.getBarbeiro().getUser() != null) {
                    alertaService.notificarUsuario(
                            ag.getBarbeiro().getUser().getId(),
                            ag.getId(),
                            "Mensagem sobre atraso",
                            resumo,
                            TipoAlerta.ATRASO
                    );
                }
            } else if (ag.getCliente() != null && ag.getCliente().getUser() != null) {
                alertaService.notificarUsuario(
                        ag.getCliente().getUser().getId(),
                        ag.getId(),
                        "Resposta sobre seu atraso",
                        texto,
                        TipoAlerta.ATRASO
                );
            }
        } catch (Exception ignored) {
        }
    }

    private void validarAcessoEquipe(Agendamento ag, Authentication auth) {
        if (AuthzHelper.isStaff(auth)) {
            return;
        }
        if (AuthzHelper.hasRole(auth, "ROLE_BARBEIRO")) {
            Barbeiro me = barbeiroService.findEntityByUserEmail(AuthzHelper.email(auth));
            UUID barbeiroId = AuthzHelper.barbeiroIdOf(ag);
            if (barbeiroId != null && barbeiroId.equals(me.getId())) {
                return;
            }
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    private void validarLeituraAtraso(Agendamento ag, Authentication auth) {
        if (AuthzHelper.isStaff(auth)) {
            return;
        }
        if (AuthzHelper.hasRole(auth, "ROLE_CLIENTE")) {
            Cliente me = clienteService.findEntityByUserEmail(AuthzHelper.email(auth));
            UUID donoId = AuthzHelper.clienteIdOf(ag);
            if (donoId != null && donoId.equals(me.getId())) {
                return;
            }
        }
        if (AuthzHelper.hasRole(auth, "ROLE_BARBEIRO")) {
            Barbeiro me = barbeiroService.findEntityByUserEmail(AuthzHelper.email(auth));
            UUID barbeiroId = AuthzHelper.barbeiroIdOf(ag);
            if (barbeiroId != null && barbeiroId.equals(me.getId())) {
                return;
            }
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }
}
