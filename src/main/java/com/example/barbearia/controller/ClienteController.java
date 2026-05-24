package com.example.barbearia.controller;

import com.example.barbearia.dto.ClienteResponseDTO;
import com.example.barbearia.dto.ClienteUpdateDTO;
import com.example.barbearia.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> meuPerfil(Authentication authentication) {
        return ResponseEntity.ok(clienteService.getMe(authentication.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> atualizarMeuPerfil(
            Authentication authentication,
            @RequestBody @Valid ClienteUpdateDTO dto
    ) {
        return ResponseEntity.ok(clienteService.updateMe(authentication.getName(), dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<List<ClienteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(clienteService.findAllDtos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(clienteService.findByIdAutorizado(id, authentication));
    }
}
