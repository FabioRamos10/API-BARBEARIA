package com.example.barbearia.controller;

import com.example.barbearia.dto.BarbeiroResponseDTO;
import com.example.barbearia.dto.BarbeiroUpdateDTO;
import com.example.barbearia.service.BarbeiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService barbeiroService;

    @GetMapping("/ativos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BarbeiroResponseDTO>> listarAtivos() {
        return ResponseEntity.ok(barbeiroService.findAllAtivosDtos());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BARBEIRO')")
    public ResponseEntity<BarbeiroResponseDTO> meuPerfil(Authentication authentication) {
        return ResponseEntity.ok(barbeiroService.getMe(authentication.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('BARBEIRO')")
    public ResponseEntity<BarbeiroResponseDTO> atualizarMeuPerfil(
            Authentication authentication,
            @RequestBody @Valid BarbeiroUpdateDTO dto
    ) {
        return ResponseEntity.ok(barbeiroService.updateMe(authentication.getName(), dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BarbeiroResponseDTO>> listarTodos() {
        return ResponseEntity.ok(barbeiroService.findAllDtos());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BarbeiroResponseDTO> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid BarbeiroUpdateDTO dto
    ) {
        return ResponseEntity.ok(barbeiroService.atualizar(id, dto));
    }
}
