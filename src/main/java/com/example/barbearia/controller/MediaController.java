package com.example.barbearia.controller;

import com.example.barbearia.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/publicacoes/{publicacaoId}/{nomeArquivo}")
    public ResponseEntity<Resource> publicacao(
            @PathVariable UUID publicacaoId,
            @PathVariable String nomeArquivo
    ) {
        return mediaService.servirPublicacao(publicacaoId, nomeArquivo);
    }

    @GetMapping("/chat/{conversaId}/{nomeArquivo}")
    public ResponseEntity<Resource> chat(
            @PathVariable UUID conversaId,
            @PathVariable String nomeArquivo,
            Authentication authentication
    ) {
        return mediaService.servirChat(conversaId, nomeArquivo, authentication);
    }
}
