package com.example.barbearia.controller;

import com.example.barbearia.dto.PagamentoCreateDTO;
import com.example.barbearia.dto.PagamentoResponseDTO;
import com.example.barbearia.service.PagamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','BARBEIRO')")
    public ResponseEntity<PagamentoResponseDTO> criar(
            @RequestBody @Valid PagamentoCreateDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagamentoService.criar(dto, authentication));
    }

    @GetMapping("/agendamento/{agendamentoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagamentoResponseDTO> porAgendamento(
            @PathVariable UUID agendamentoId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(pagamentoService.buscarPorAgendamento(agendamentoId, authentication));
    }

    @PostMapping(value = "/{id}/comprovante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PagamentoResponseDTO> enviarComprovante(
            @PathVariable UUID id,
            @RequestPart("arquivo") MultipartFile arquivo,
            Authentication authentication
    ) {
        return ResponseEntity.ok(pagamentoService.enviarComprovante(id, arquivo, authentication));
    }

    @GetMapping("/{id}/comprovante")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> baixarComprovante(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        Resource resource = pagamentoService.baixarComprovante(id, authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"comprovante\"")
                .body(resource);
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','BARBEIRO')")
    public ResponseEntity<PagamentoResponseDTO> confirmar(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(pagamentoService.confirmarPagamento(id, authentication));
    }
}
