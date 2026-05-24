package com.example.barbearia.controller;

import com.example.barbearia.dto.AlertaResponseDTO;
import com.example.barbearia.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AlertaResponseDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(alertaService.listarMeus(authentication));
    }

    @GetMapping("/nao-lidos/contagem")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> contarNaoLidos(Authentication authentication) {
        return ResponseEntity.ok(Map.of("total", alertaService.contarNaoLidos(authentication)));
    }

    @PatchMapping("/{id}/lido")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarLido(@PathVariable UUID id, Authentication authentication) {
        alertaService.marcarLido(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
