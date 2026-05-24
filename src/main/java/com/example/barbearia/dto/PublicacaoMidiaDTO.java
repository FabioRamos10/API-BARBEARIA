package com.example.barbearia.dto;

import com.example.barbearia.domain.PublicacaoMidia;
import com.example.barbearia.domain.TipoMidiaPublicacao;

import java.util.UUID;

public record PublicacaoMidiaDTO(
        UUID id,
        TipoMidiaPublicacao tipo,
        String url,
        String contentType,
        String nomeArquivo,
        int ordem
) {
    public static PublicacaoMidiaDTO from(PublicacaoMidia m) {
        return new PublicacaoMidiaDTO(
                m.getId(),
                m.getTipo(),
                m.getUrlPublica(),
                m.getContentType(),
                m.getNomeArquivo(),
                m.getOrdem()
        );
    }
}
