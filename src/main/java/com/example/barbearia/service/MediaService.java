package com.example.barbearia.service;

import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.ConversaRepository;
import com.example.barbearia.repository.PublicacaoRepository;
import com.example.barbearia.security.AuthzHelper;
import com.example.barbearia.storage.ArquivoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final ArquivoStorageService arquivoStorageService;
    private final PublicacaoRepository publicacaoRepository;
    private final ConversaRepository conversaRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> servirPublicacao(UUID publicacaoId, String nomeArquivo) {
        publicacaoRepository.findById(publicacaoId)
                .filter(p -> Boolean.TRUE.equals(p.getPublicado()))
                .orElseThrow(() -> new ApiException("Publicação não encontrada", HttpStatus.NOT_FOUND));
        return servir("publicacoes", publicacaoId, nomeArquivo);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> servirChat(UUID conversaId, String nomeArquivo, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException("Autenticação necessária", HttpStatus.UNAUTHORIZED);
        }
        var me = userService.requireByEmail(AuthzHelper.email(auth));
        var conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new ApiException("Conversa não encontrada", HttpStatus.NOT_FOUND));
        if (!conversa.getUsuarioMenorId().equals(me.getId()) && !conversa.getUsuarioMaiorId().equals(me.getId())) {
            throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
        }
        return servir("chat", conversaId, nomeArquivo);
    }

    private ResponseEntity<Resource> servir(String categoria, UUID ownerId, String nomeArquivo) {
        Path path = arquivoStorageService.resolverMidia(categoria, ownerId, nomeArquivo);
        try {
            Resource resource = new UrlResource(path.toUri());
            String contentType = java.nio.file.Files.probeContentType(path);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeArquivo + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            throw new ApiException("Falha ao ler arquivo", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
