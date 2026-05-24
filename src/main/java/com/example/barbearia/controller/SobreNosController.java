package com.example.barbearia.controller;

import com.example.barbearia.domain.TipoPublicacao;
import com.example.barbearia.dto.PublicacaoCreateDTO;
import com.example.barbearia.dto.PublicacaoResponseDTO;
import com.example.barbearia.service.PublicacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sobre-nos")
@RequiredArgsConstructor
public class SobreNosController {

    private final PublicacaoService publicacaoService;

    @GetMapping("/publicacoes")
    public ResponseEntity<List<PublicacaoResponseDTO>> feed() {
        return ResponseEntity.ok(publicacaoService.listarPublicadas());
    }

    @GetMapping("/publicacoes/tipo/{tipo}")
    public ResponseEntity<List<PublicacaoResponseDTO>> porTipo(@PathVariable TipoPublicacao tipo) {
        return ResponseEntity.ok(publicacaoService.listarPorTipo(tipo));
    }

    @GetMapping("/admin/publicacoes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PublicacaoResponseDTO>> listarAdmin() {
        return ResponseEntity.ok(publicacaoService.listarTodasAdmin());
    }

    @PostMapping("/admin/publicacoes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PublicacaoResponseDTO> criar(
            @RequestBody @Valid PublicacaoCreateDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicacaoService.criar(dto, authentication));
    }

    @PostMapping(value = "/admin/publicacoes/com-midias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PublicacaoResponseDTO> criarComMidias(
            @RequestPart("dados") @Valid PublicacaoCreateDTO dto,
            @RequestPart(value = "capa", required = false) MultipartFile capa,
            @RequestPart(value = "anexos", required = false) List<MultipartFile> anexos,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicacaoService.criarComArquivos(dto, capa, anexos, authentication));
    }

    @PutMapping("/admin/publicacoes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PublicacaoResponseDTO> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid PublicacaoCreateDTO dto
    ) {
        return ResponseEntity.ok(publicacaoService.atualizar(id, dto));
    }

    @PutMapping(value = "/admin/publicacoes/{id}/com-midias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PublicacaoResponseDTO> atualizarComMidias(
            @PathVariable UUID id,
            @RequestPart("dados") @Valid PublicacaoCreateDTO dto,
            @RequestPart(value = "capa", required = false) MultipartFile capa,
            @RequestPart(value = "anexos", required = false) List<MultipartFile> anexos
    ) {
        return ResponseEntity.ok(publicacaoService.atualizarComArquivos(id, dto, capa, anexos));
    }

    @DeleteMapping("/admin/publicacoes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        publicacaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
