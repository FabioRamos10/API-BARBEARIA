package com.example.barbearia.controller;

import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.dto.AgendaDisponivelResponseDTO;
import com.example.barbearia.dto.AgendamentoAtrasoDTO;
import com.example.barbearia.dto.AgendamentoCreateDTO;
import com.example.barbearia.dto.AgendamentoResponseDTO;
import com.example.barbearia.dto.AtrasoMensagemCreateDTO;
import com.example.barbearia.dto.AtrasoMensagemResponseDTO;
import com.example.barbearia.service.AgendamentoService;
import com.example.barbearia.service.AtrasoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final AtrasoService atrasoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','CLIENTE')")
    public ResponseEntity<AgendamentoResponseDTO> criar(
            @RequestBody @Valid AgendamentoCreateDTO dto,
            Authentication authentication
    ) {
        AgendamentoResponseDTO response = agendamentoService.criar(dto, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/disponibilidade")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','CLIENTE')")
    public ResponseEntity<AgendaDisponivelResponseDTO> listarDisponibilidade(
            @RequestParam UUID barbeiroId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) UUID servicoId,
            @RequestParam(required = false) List<UUID> servicoIds
    ) {
        return ResponseEntity.ok(
                agendamentoService.listarHorariosDisponiveis(barbeiroId, data, servicoId, servicoIds)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgendamentoResponseDTO> findById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(agendamentoService.findById(id, authentication));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<List<AgendamentoResponseDTO>> findAll() {
        return ResponseEntity.ok(agendamentoService.findAll());
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','CLIENTE')")
    public ResponseEntity<List<AgendamentoResponseDTO>> findByCliente(
            @PathVariable UUID clienteId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(agendamentoService.findByCliente(clienteId, authentication));
    }

    @GetMapping("/barbeiro/{barbeiroId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','BARBEIRO')")
    public ResponseEntity<List<AgendamentoResponseDTO>> findByBarbeiro(
            @PathVariable UUID barbeiroId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(agendamentoService.findByBarbeiro(barbeiroId, authentication));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','BARBEIRO')")
    public ResponseEntity<AgendamentoResponseDTO> atualizarStatus(
            @PathVariable UUID id,
            @RequestParam StatusAgendamento status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(agendamentoService.atualizarStatus(id, status, authentication));
    }

    @PostMapping("/{id}/atraso")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<AgendamentoResponseDTO> informarAtraso(
            @PathVariable UUID id,
            @RequestBody @Valid AgendamentoAtrasoDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(agendamentoService.registrarAtraso(id, dto, authentication));
    }

    @PostMapping("/{id}/atraso/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','BARBEIRO')")
    public ResponseEntity<AgendamentoResponseDTO> confirmarAtraso(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        atrasoService.confirmarAtraso(id, authentication);
        return ResponseEntity.ok(agendamentoService.findById(id, authentication));
    }

    @GetMapping("/{id}/atraso/mensagens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AtrasoMensagemResponseDTO>> mensagensAtraso(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(atrasoService.listarMensagens(id, authentication));
    }

    @PostMapping("/{id}/atraso/mensagens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AtrasoMensagemResponseDTO> enviarMensagemAtraso(
            @PathVariable UUID id,
            @RequestBody @Valid AtrasoMensagemCreateDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(atrasoService.enviarMensagem(id, dto, authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','CLIENTE','BARBEIRO')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id, Authentication authentication) {
        agendamentoService.cancelar(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
