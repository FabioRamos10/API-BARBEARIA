package com.example.barbearia.controller;

import com.example.barbearia.dto.*;
import com.example.barbearia.service.ChatService;
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
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/usuarios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UsuarioChatDTO>> listarUsuarios(Authentication authentication) {
        return ResponseEntity.ok(chatService.listarUsuarios(authentication));
    }

    @GetMapping("/conversas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversaResponseDTO>> listarConversas(Authentication authentication) {
        return ResponseEntity.ok(chatService.listarConversas(authentication));
    }

    @PostMapping("/conversas/{outroUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversaResponseDTO> abrirConversa(
            @PathVariable UUID outroUserId,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.abrirConversa(outroUserId, authentication));
    }

    @GetMapping("/conversas/{conversaId}/mensagens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMensagemResponseDTO>> mensagens(
            @PathVariable UUID conversaId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(chatService.listarMensagens(conversaId, authentication));
    }

    @PostMapping("/conversas/{conversaId}/mensagens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMensagemResponseDTO> enviar(
            @PathVariable UUID conversaId,
            @RequestBody @Valid ChatEnviarDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.enviar(conversaId, dto, authentication));
    }

    @PostMapping(value = "/conversas/{conversaId}/mensagens/com-anexo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMensagemResponseDTO> enviarComAnexo(
            @PathVariable UUID conversaId,
            @RequestPart(value = "texto", required = false) String texto,
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivo,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.enviarComArquivo(conversaId, texto, arquivo, authentication));
    }

    @PatchMapping("/conversas/{conversaId}/lidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarLidas(@PathVariable UUID conversaId, Authentication authentication) {
        chatService.marcarMensagensLidas(conversaId, authentication);
        return ResponseEntity.noContent().build();
    }
}
