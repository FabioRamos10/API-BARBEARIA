package com.example.barbearia.controller;

import com.example.barbearia.dto.AvaliacaoCreateDTO;
import com.example.barbearia.dto.AvaliacaoResponseDTO;
import com.example.barbearia.dto.AvaliacoesBarbeiroDTO;
import com.example.barbearia.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<AvaliacaoResponseDTO> criar(
            @RequestBody @Valid AvaliacaoCreateDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(avaliacaoService.criar(dto, authentication));
    }

    @GetMapping("/agendamento/{agendamentoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvaliacaoResponseDTO> porAgendamento(
            @PathVariable UUID agendamentoId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(avaliacaoService.buscarPorAgendamento(agendamentoId, authentication));
    }

    @GetMapping("/barbeiro/{barbeiroId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvaliacoesBarbeiroDTO> porBarbeiro(@PathVariable UUID barbeiroId) {
        return ResponseEntity.ok(avaliacaoService.listarPorBarbeiro(barbeiroId));
    }
}
