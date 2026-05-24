package com.example.barbearia.service;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.Avaliacao;
import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.dto.AvaliacaoCreateDTO;
import com.example.barbearia.dto.AvaliacaoResponseDTO;
import com.example.barbearia.dto.AvaliacoesBarbeiroDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.AgendamentoRepository;
import com.example.barbearia.repository.AvaliacaoRepository;
import com.example.barbearia.repository.BarbeiroRepository;
import com.example.barbearia.security.AuthzHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteService clienteService;

    @Transactional
    public AvaliacaoResponseDTO criar(AvaliacaoCreateDTO dto, Authentication authentication) {
        if (!AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            throw new ApiException("Apenas clientes podem avaliar atendimentos", HttpStatus.FORBIDDEN);
        }

        Agendamento agendamento = agendamentoRepository.findById(dto.agendamentoId())
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));

        var cliente = clienteService.findEntityByUserEmail(authentication.getName());
        UUID donoId = AuthzHelper.clienteIdOf(agendamento);
        if (donoId == null || !donoId.equals(cliente.getId())) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }

        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) {
            throw new ApiException(
                    "Só é possível avaliar agendamentos concluídos",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (avaliacaoRepository.existsByAgendamento_Id(agendamento.getId())) {
            throw new ApiException("Este agendamento já foi avaliado", HttpStatus.CONFLICT);
        }

        Avaliacao avaliacao = Avaliacao.builder()
                .agendamento(agendamento)
                .nota(dto.nota())
                .comentario(dto.comentario())
                .build();

        return AvaliacaoResponseDTO.from(avaliacaoRepository.save(avaliacao));
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponseDTO buscarPorAgendamento(UUID agendamentoId, Authentication authentication) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));
        validarLeituraAgendamento(agendamento, authentication);

        Avaliacao avaliacao = avaliacaoRepository.findByAgendamento_Id(agendamentoId)
                .orElseThrow(() -> new ApiException("Avaliação não encontrada", HttpStatus.NOT_FOUND));

        return AvaliacaoResponseDTO.from(avaliacao);
    }

    @Transactional(readOnly = true)
    public AvaliacoesBarbeiroDTO listarPorBarbeiro(UUID barbeiroId) {
        Barbeiro barbeiro = barbeiroRepository.findById(barbeiroId)
                .orElseThrow(() -> new ApiException("Barbeiro não encontrado", HttpStatus.NOT_FOUND));

        List<AvaliacaoResponseDTO> itens = avaliacaoRepository.findByBarbeiro(barbeiro).stream()
                .map(AvaliacaoResponseDTO::from)
                .collect(Collectors.toList());

        Double media = avaliacaoRepository.calcularMediaAvaliacoesPorBarbeiro(barbeiroId);

        return new AvaliacoesBarbeiroDTO(
                barbeiro.getId(),
                barbeiro.getNome(),
                media,
                itens.size(),
                itens
        );
    }

    private void validarLeituraAgendamento(Agendamento agendamento, Authentication authentication) {
        if (AuthzHelper.isStaff(authentication)) {
            return;
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            var cliente = clienteService.findEntityByUserEmail(authentication.getName());
            UUID donoId = AuthzHelper.clienteIdOf(agendamento);
            if (donoId != null && donoId.equals(cliente.getId())) {
                return;
            }
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_BARBEIRO")) {
            var me = barbeiroRepository.findByUserEmail(AuthzHelper.email(authentication))
                    .orElse(null);
            UUID barbeiroId = AuthzHelper.barbeiroIdOf(agendamento);
            if (me != null && barbeiroId != null && barbeiroId.equals(me.getId())) {
                return;
            }
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }
}
