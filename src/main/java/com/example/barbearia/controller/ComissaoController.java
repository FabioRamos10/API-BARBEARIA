package com.example.barbearia.controller;

import com.example.barbearia.dto.comissao.FolhaComissaoResponseDTO;
import com.example.barbearia.dto.comissao.FolhaComissaoStatusDTO;
import com.example.barbearia.service.ComissaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comissoes")
@RequiredArgsConstructor
public class ComissaoController {

    private final ComissaoService comissaoService;

    @GetMapping("/folhas")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','BARBEIRO')")
    public ResponseEntity<List<FolhaComissaoResponseDTO>> listarFolhas(
            @RequestParam(required = false) String anoMes,
            @RequestParam(required = false) UUID barbeiroId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(comissaoService.listarFolhas(anoMes, barbeiroId, authentication));
    }

    @GetMapping("/meses")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
    public ResponseEntity<List<String>> listarMeses(Authentication authentication) {
        return ResponseEntity.ok(comissaoService.listarMesesRegistrados(authentication));
    }

    @PatchMapping("/folhas/{folhaId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FolhaComissaoResponseDTO> atualizarStatus(
            @PathVariable UUID folhaId,
            @RequestBody @Valid FolhaComissaoStatusDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(comissaoService.atualizarStatusFolha(folhaId, dto, authentication));
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','BARBEIRO')")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) String anoMes,
            @RequestParam(required = false) UUID barbeiroId,
            @RequestParam(defaultValue = "false") boolean todos,
            Authentication authentication
    ) {
        var arquivo = comissaoService.gerarPdf(anoMes, barbeiroId, todos, authentication);
        String nome = "comissoes" + (anoMes != null ? "-" + anoMes : "") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .body(arquivo);
    }
}
