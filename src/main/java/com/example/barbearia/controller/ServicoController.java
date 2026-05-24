package com.example.barbearia.controller;

import com.example.barbearia.dto.ServicoCreateDTO;
import com.example.barbearia.dto.ServicoResponseDTO;
import com.example.barbearia.dto.ServicoUpdateDTO;
import com.example.barbearia.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServicoResponseDTO>> listar(
            @RequestParam(required = false) String categoria
    ) {
        if (categoria != null && !categoria.isBlank()) {
            return ResponseEntity.ok(servicoService.findByCategoriaDto(categoria.trim()));
        }
        return ResponseEntity.ok(servicoService.findAllAtivosDtos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ServicoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(servicoService.findDtoById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicoResponseDTO> criar(@RequestBody @Valid ServicoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoService.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicoResponseDTO> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid ServicoUpdateDTO dto
    ) {
        return ResponseEntity.ok(servicoService.atualizar(id, dto));
    }
}
