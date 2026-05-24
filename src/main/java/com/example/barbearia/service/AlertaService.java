package com.example.barbearia.service;

import com.example.barbearia.domain.AlertaSistema;
import com.example.barbearia.domain.TipoAlerta;
import com.example.barbearia.domain.User;
import com.example.barbearia.dto.AlertaResponseDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.AlertaSistemaRepository;
import com.example.barbearia.repository.UserRepository;
import com.example.barbearia.security.AuthzHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaSistemaRepository alertaRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public void criar(UUID destinatarioUserId, String titulo, String mensagem, TipoAlerta tipo, UUID referenciaId) {
        alertaRepository.save(AlertaSistema.builder()
                .destinatarioUserId(destinatarioUserId)
                .titulo(titulo)
                .mensagem(mensagem)
                .tipo(tipo)
                .referenciaId(referenciaId)
                .lido(false)
                .build());
    }

    @Transactional
    public void notificarGestao(UUID agendamentoId, String titulo, String mensagem) {
        for (User u : userRepository.findByRoleInAndAtivoTrue(
                List.of(com.example.barbearia.domain.Role.ADMIN, com.example.barbearia.domain.Role.RECEPCIONISTA))) {
            criar(u.getId(), titulo, mensagem, TipoAlerta.ATRASO, agendamentoId);
        }
    }

    @Transactional
    public void notificarUsuario(UUID userId, UUID agendamentoId, String titulo, String mensagem, TipoAlerta tipo) {
        criar(userId, titulo, mensagem, tipo, agendamentoId);
    }

    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarMeus(Authentication auth) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        return alertaRepository.findByDestinatarioUserIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(AlertaResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidos(Authentication auth) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        return alertaRepository.countByDestinatarioUserIdAndLidoFalse(me.getId());
    }

    @Transactional
    public void marcarLido(UUID alertaId, Authentication auth) {
        User me = userService.requireByEmail(AuthzHelper.email(auth));
        AlertaSistema alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new ApiException("Alerta não encontrado", HttpStatus.NOT_FOUND));
        if (!alerta.getDestinatarioUserId().equals(me.getId())) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        alerta.setLido(true);
        alertaRepository.save(alerta);
    }
}
