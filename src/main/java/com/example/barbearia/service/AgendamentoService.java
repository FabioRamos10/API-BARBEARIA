package com.example.barbearia.service;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.AgendamentoItem;
import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.Cliente;
import com.example.barbearia.domain.Servico;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.dto.AgendaDisponivelResponseDTO;
import com.example.barbearia.dto.AgendamentoAtrasoDTO;
import com.example.barbearia.dto.AgendamentoCreateDTO;
import com.example.barbearia.dto.AgendamentoResponseDTO;
import com.example.barbearia.dto.HorarioDisponivelDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.AgendamentoRepository;
import com.example.barbearia.security.AuthzHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteService clienteService;
    private final BarbeiroService barbeiroService;
    private final ServicoService servicoService;
    private final NotificacaoService notificacaoService;
    private final PagamentoService pagamentoService;
    private final AtrasoService atrasoService;

    private static final LocalTime HORARIO_ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORARIO_FECHAMENTO = LocalTime.of(18, 0);
    private static final int INTERVALO_SLOT_MINUTOS = 15;
    private static final List<StatusAgendamento> STATUS_OCUPADO = List.of(
            StatusAgendamento.AGENDADO,
            StatusAgendamento.CONFIRMADO,
            StatusAgendamento.EM_ANDAMENTO
    );

    @Transactional
    public AgendamentoResponseDTO criar(AgendamentoCreateDTO dto, Authentication authentication) {
        Cliente cliente = clienteService.findById(dto.getClienteId());
        validarCriacao(cliente, authentication);

        Barbeiro barbeiro = barbeiroService.findById(dto.getBarbeiroId());
        List<Servico> servicos = resolverServicos(dto);

        validarHorarioFuncionamento(dto.getInicio());
        int duracaoTotal = servicos.stream().mapToInt(Servico::getDuracaoMinutos).sum();
        LocalDateTime fim = dto.getInicio().plusMinutes(duracaoTotal);
        validarHorarioFechamento(fim);
        validarConflitoHorario(barbeiro.getId(), dto.getInicio(), fim);

        Agendamento agendamento = Agendamento.builder()
                .inicio(dto.getInicio())
                .fim(fim)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servicos.get(0))
                .status(StatusAgendamento.AGENDADO)
                .observacoes(dto.getObservacoes())
                .build();

        int ordem = 0;
        for (Servico servico : servicos) {
            agendamento.getItens().add(AgendamentoItem.builder()
                    .agendamento(agendamento)
                    .servico(servico)
                    .ordem(ordem++)
                    .build());
        }

        agendamento = agendamentoRepository.save(agendamento);

        try {
            notificacaoService.notificarAgendamentoCriado(agendamento.getId());
        } catch (Exception ignored) {
        }

        return toResponseDTO(agendamento);
    }

    @Transactional(readOnly = true)
    public AgendamentoResponseDTO findById(UUID id, Authentication authentication) {
        Agendamento agendamento = buscarOu404(id);
        validarLeitura(agendamento, authentication);
        return toResponseDTO(agendamento);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> findAll() {
        return agendamentoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> findByCliente(UUID clienteId, Authentication authentication) {
        validarConsultaPorCliente(clienteId, authentication);
        Cliente cliente = clienteService.findById(clienteId);
        return agendamentoRepository.findByClienteOrderByInicioDesc(cliente).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgendaDisponivelResponseDTO listarHorariosDisponiveis(
            UUID barbeiroId,
            LocalDate data,
            UUID servicoId,
            List<UUID> servicoIds
    ) {
        barbeiroService.findById(barbeiroId);

        if (data.isBefore(LocalDate.now())) {
            return agendaVazia(barbeiroId, data, 0);
        }

        List<Servico> servicos = resolverServicosParaConsulta(servicoId, servicoIds);
        int duracaoTotal = servicos.stream().mapToInt(Servico::getDuracaoMinutos).sum();

        LocalDateTime diaInicio = data.atTime(HORARIO_ABERTURA);
        LocalDateTime diaFim = data.atTime(HORARIO_FECHAMENTO);

        List<Agendamento> ocupados = agendamentoRepository.findOcupadosBarbeiroNoPeriodo(
                barbeiroId, diaInicio, diaFim, STATUS_OCUPADO);

        LocalDateTime candidatoMinimo = calcularInicioMinimo(data);
        List<HorarioDisponivelDTO> horarios = new ArrayList<>();

        LocalDateTime candidato = diaInicio;
        while (!candidato.isAfter(diaFim)) {
            LocalDateTime fim = candidato.plusMinutes(duracaoTotal);

            if (slotValido(candidato, fim, candidatoMinimo)
                    && !temConflito(candidato, fim, ocupados)) {
                horarios.add(HorarioDisponivelDTO.builder()
                        .inicio(candidato)
                        .fim(fim)
                        .build());
            }

            candidato = candidato.plusMinutes(INTERVALO_SLOT_MINUTOS);
            if (candidato.toLocalTime().isAfter(HORARIO_FECHAMENTO)) {
                break;
            }
        }

        return AgendaDisponivelResponseDTO.builder()
                .barbeiroId(barbeiroId)
                .data(data)
                .duracaoTotalMinutos(duracaoTotal)
                .abertura(HORARIO_ABERTURA)
                .fechamento(HORARIO_FECHAMENTO)
                .intervaloMinutos(INTERVALO_SLOT_MINUTOS)
                .horariosDisponiveis(horarios)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> findByBarbeiro(UUID barbeiroId, Authentication authentication) {
        validarConsultaPorBarbeiro(barbeiroId, authentication);
        Barbeiro barbeiro = barbeiroService.findById(barbeiroId);
        return agendamentoRepository.findByBarbeiroOrderByInicioDesc(barbeiro).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AgendamentoResponseDTO atualizarStatus(
            UUID id,
            StatusAgendamento novoStatus,
            Authentication authentication
    ) {
        Agendamento agendamento = buscarOu404(id);
        validarAtualizacaoStatus(agendamento, authentication);

        agendamento.setStatus(novoStatus);
        agendamento = agendamentoRepository.save(agendamento);

        try {
            notificacaoService.notificarMudancaStatus(agendamento.getId(), novoStatus);
        } catch (Exception ignored) {
        }

        if (novoStatus == StatusAgendamento.CONCLUIDO) {
            try {
                pagamentoService.processarAoConcluir(agendamento.getId());
            } catch (Exception ignored) {
            }
        }

        return toResponseDTO(agendamento);
    }

    @Transactional
    public AgendamentoResponseDTO registrarAtraso(UUID id, AgendamentoAtrasoDTO dto, Authentication authentication) {
        Agendamento agendamento = buscarOu404(id);
        validarAtrasoCliente(agendamento, authentication);

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO
                || agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new ApiException("Não é possível informar atraso para este agendamento", HttpStatus.BAD_REQUEST);
        }

        agendamento.setAtrasoMinutos(dto.minutos());
        agendamento.setAtrasoMotivo(dto.motivo().trim());
        agendamento.setAtrasoInformadoEm(LocalDateTime.now());
        agendamento = agendamentoRepository.save(agendamento);

        try {
            notificacaoService.notificarAtraso(agendamento.getId(), dto.minutos(), dto.motivo().trim());
        } catch (Exception ignored) {
        }

        agendamentoRepository.findByIdComDetalhes(agendamento.getId()).ifPresent(ag -> {
            try {
                atrasoService.aposInformarAtraso(ag, dto.minutos(), dto.motivo().trim());
            } catch (Exception ignored) {
            }
        });

        return toResponseDTO(agendamento);
    }

    private List<Servico> resolverServicos(AgendamentoCreateDTO dto) {
        return resolverServicosParaConsulta(dto.getServicoId(), dto.getServicoIds());
    }

    private List<Servico> resolverServicosParaConsulta(UUID servicoId, List<UUID> servicoIds) {
        List<UUID> ids = servicoIds;
        if (ids == null || ids.isEmpty()) {
            if (servicoId == null) {
                throw new ApiException("Informe ao menos um serviço (servicoId ou servicoIds)", HttpStatus.BAD_REQUEST);
            }
            ids = List.of(servicoId);
        }
        List<Servico> servicos = new ArrayList<>();
        for (UUID id : ids) {
            servicos.add(servicoService.findById(id));
        }
        return servicos;
    }

    private AgendaDisponivelResponseDTO agendaVazia(UUID barbeiroId, LocalDate data, int duracaoTotal) {
        return AgendaDisponivelResponseDTO.builder()
                .barbeiroId(barbeiroId)
                .data(data)
                .duracaoTotalMinutos(duracaoTotal)
                .abertura(HORARIO_ABERTURA)
                .fechamento(HORARIO_FECHAMENTO)
                .intervaloMinutos(INTERVALO_SLOT_MINUTOS)
                .horariosDisponiveis(List.of())
                .build();
    }

    private LocalDateTime calcularInicioMinimo(LocalDate data) {
        if (!data.equals(LocalDate.now())) {
            return LocalDateTime.MIN;
        }

        LocalDateTime agora = LocalDateTime.now().withSecond(0).withNano(0);
        int minuto = agora.getMinute();
        int resto = minuto % INTERVALO_SLOT_MINUTOS;
        if (resto != 0) {
            agora = agora.plusMinutes(INTERVALO_SLOT_MINUTOS - resto);
        }
        return agora;
    }

    private boolean slotValido(LocalDateTime inicio, LocalDateTime fim, LocalDateTime inicioMinimo) {
        LocalTime horarioInicio = inicio.toLocalTime();
        if (horarioInicio.isBefore(HORARIO_ABERTURA) || horarioInicio.isAfter(HORARIO_FECHAMENTO)) {
            return false;
        }
        if (fim.toLocalTime().isAfter(HORARIO_FECHAMENTO)) {
            return false;
        }
        return inicio.isAfter(inicioMinimo) || inicio.isEqual(inicioMinimo);
    }

    private boolean temConflito(LocalDateTime inicio, LocalDateTime fim, List<Agendamento> ocupados) {
        for (Agendamento agendamento : ocupados) {
            if (agendamento.getInicio().isBefore(fim) && agendamento.getFim().isAfter(inicio)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void cancelar(UUID id, Authentication authentication) {
        Agendamento agendamento = buscarOu404(id);
        validarCancelamento(agendamento, authentication);

        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new ApiException("Não é possível cancelar um agendamento já concluído", HttpStatus.BAD_REQUEST);
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new ApiException("Agendamento já está cancelado", HttpStatus.BAD_REQUEST);
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);

        try {
            notificacaoService.notificarAgendamentoCancelado(agendamento.getId());
        } catch (Exception ignored) {
        }
    }

    // ===================== AUTORIZAÇÃO =====================

    private void validarCriacao(Cliente cliente, Authentication authentication) {
        if (AuthzHelper.isStaff(authentication)) {
            return;
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            Cliente me = clienteService.findEntityByUserEmail(AuthzHelper.email(authentication));
            if (!me.getId().equals(cliente.getId())) {
                throw new ApiException("Cliente só pode agendar para a própria conta", HttpStatus.FORBIDDEN);
            }
            return;
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    private void validarLeitura(Agendamento agendamento, Authentication authentication) {
        if (AuthzHelper.isStaff(authentication)) {
            return;
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            Cliente me = clienteService.findEntityByUserEmail(AuthzHelper.email(authentication));
            if (agendamento.getClienteId() != null && agendamento.getClienteId().equals(me.getId())) {
                return;
            }
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_BARBEIRO")) {
            Barbeiro me = barbeiroService.findEntityByUserEmail(AuthzHelper.email(authentication));
            if (agendamento.getBarbeiroId() != null && agendamento.getBarbeiroId().equals(me.getId())) {
                return;
            }
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    private void validarConsultaPorCliente(UUID clienteId, Authentication authentication) {
        if (AuthzHelper.isStaff(authentication)) {
            return;
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            Cliente me = clienteService.findEntityByUserEmail(AuthzHelper.email(authentication));
            if (!me.getId().equals(clienteId)) {
                throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
            }
            return;
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    private void validarConsultaPorBarbeiro(UUID barbeiroId, Authentication authentication) {
        if (AuthzHelper.isStaff(authentication)) {
            return;
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_BARBEIRO")) {
            Barbeiro me = barbeiroService.findEntityByUserEmail(AuthzHelper.email(authentication));
            if (!me.getId().equals(barbeiroId)) {
                throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
            }
            return;
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    private void validarAtualizacaoStatus(Agendamento agendamento, Authentication authentication) {
        if (AuthzHelper.isStaff(authentication)) {
            return;
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_BARBEIRO")) {
            Barbeiro me = barbeiroService.findEntityByUserEmail(AuthzHelper.email(authentication));
            if (agendamento.getBarbeiroId() != null && agendamento.getBarbeiroId().equals(me.getId())) {
                return;
            }
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    private void validarAtrasoCliente(Agendamento agendamento, Authentication authentication) {
        if (!AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            throw new ApiException("Somente o cliente pode informar atraso", HttpStatus.FORBIDDEN);
        }
        Cliente me = clienteService.findEntityByUserEmail(AuthzHelper.email(authentication));
        UUID donoId = AuthzHelper.clienteIdOf(agendamento);
        if (donoId == null || !donoId.equals(me.getId())) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
    }

    private void validarCancelamento(Agendamento agendamento, Authentication authentication) {
        if (AuthzHelper.isStaff(authentication)) {
            return;
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_CLIENTE")) {
            Cliente me = clienteService.findEntityByUserEmail(AuthzHelper.email(authentication));
            if (agendamento.getClienteId() != null && agendamento.getClienteId().equals(me.getId())) {
                return;
            }
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        if (AuthzHelper.hasRole(authentication, "ROLE_BARBEIRO")) {
            Barbeiro me = barbeiroService.findEntityByUserEmail(AuthzHelper.email(authentication));
            if (agendamento.getBarbeiroId() != null && agendamento.getBarbeiroId().equals(me.getId())) {
                return;
            }
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    private Agendamento buscarOu404(UUID id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new ApiException("Agendamento não encontrado", HttpStatus.NOT_FOUND));
    }

    private void enrichDetalhes(Agendamento agendamento) {
        if (agendamento.getCliente() == null && agendamento.getClienteId() != null) {
            agendamento.setCliente(clienteService.findById(agendamento.getClienteId()));
        }
        if (agendamento.getBarbeiro() == null && agendamento.getBarbeiroId() != null) {
            agendamento.setBarbeiro(barbeiroService.findById(agendamento.getBarbeiroId()));
        }
        if (agendamento.getServico() == null && agendamento.getServicoId() != null) {
            agendamento.setServico(servicoService.findById(agendamento.getServicoId()));
        }
        if (agendamento.getItens() != null) {
            for (AgendamentoItem item : agendamento.getItens()) {
                if (item.getServico() == null && item.getServicoId() != null) {
                    item.setServico(servicoService.findById(item.getServicoId()));
                }
            }
        }
    }

    // ===================== VALIDAÇÕES DE NEGÓCIO =====================

    private void validarHorarioFuncionamento(LocalDateTime inicio) {
        LocalTime horario = inicio.toLocalTime();

        if (horario.isBefore(HORARIO_ABERTURA) || horario.isAfter(HORARIO_FECHAMENTO)) {
            throw new ApiException(
                    String.format("Horário fora do funcionamento. Funcionamento: %s às %s",
                            HORARIO_ABERTURA, HORARIO_FECHAMENTO),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validarHorarioFechamento(LocalDateTime fim) {
        LocalTime horarioFim = fim.toLocalTime();

        if (horarioFim.isAfter(HORARIO_FECHAMENTO)) {
            throw new ApiException(
                    String.format("O agendamento não pode terminar após o horário de fechamento (%s)",
                            HORARIO_FECHAMENTO),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validarConflitoHorario(UUID barbeiroId, LocalDateTime inicio, LocalDateTime fim) {
        boolean existeConflito = agendamentoRepository.existsConflitoHorario(
                barbeiroId, inicio, fim, STATUS_OCUPADO
        );

        if (existeConflito) {
            throw new ApiException(
                    "Já existe um agendamento neste horário para este barbeiro",
                    HttpStatus.CONFLICT
            );
        }
    }

    private AgendamentoResponseDTO toResponseDTO(Agendamento agendamento) {
        enrichDetalhes(agendamento);
        List<AgendamentoResponseDTO.ServicoInfoDTO> servicosDto = new ArrayList<>();
        if (agendamento.getItens() != null && !agendamento.getItens().isEmpty()) {
            for (AgendamentoItem item : agendamento.getItens()) {
                servicosDto.add(toServicoInfo(item.getServico()));
            }
        } else if (agendamento.getServico() != null) {
            servicosDto.add(toServicoInfo(agendamento.getServico()));
        }

        AgendamentoResponseDTO.ServicoInfoDTO principal = servicosDto.isEmpty()
                ? null
                : servicosDto.get(0);

        return AgendamentoResponseDTO.builder()
                .id(agendamento.getId())
                .inicio(agendamento.getInicio())
                .fim(agendamento.getFim())
                .status(agendamento.getStatus())
                .observacoes(agendamento.getObservacoes())
                .atrasoMinutos(agendamento.getAtrasoMinutos())
                .atrasoMotivo(agendamento.getAtrasoMotivo())
                .atrasoInformadoEm(agendamento.getAtrasoInformadoEm())
                .atrasoStatus(agendamento.getAtrasoStatus())
                .atrasoConfirmadoEm(agendamento.getAtrasoConfirmadoEm())
                .valorTotal(agendamento.valorTotalServicos())
                .createdAt(agendamento.getCreatedAt())
                .cliente(AgendamentoResponseDTO.ClienteInfoDTO.builder()
                        .id(agendamento.getCliente().getId())
                        .nome(agendamento.getCliente().getNome())
                        .email(agendamento.getCliente().getEmail())
                        .telefone(agendamento.getCliente().getTelefone())
                        .build())
                .barbeiro(AgendamentoResponseDTO.BarbeiroInfoDTO.builder()
                        .id(agendamento.getBarbeiro().getId())
                        .nome(agendamento.getBarbeiro().getNome())
                        .telefone(agendamento.getBarbeiro().getTelefone())
                        .build())
                .servico(principal)
                .servicos(servicosDto)
                .build();
    }

    private static AgendamentoResponseDTO.ServicoInfoDTO toServicoInfo(Servico servico) {
        return AgendamentoResponseDTO.ServicoInfoDTO.builder()
                .id(servico.getId())
                .nome(servico.getNome())
                .descricao(servico.getDescricao())
                .build();
    }
}
