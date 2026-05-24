package com.example.barbearia.dto;

import com.example.barbearia.domain.Publicacao;
import com.example.barbearia.domain.TipoPublicacao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PublicacaoResponseDTO(
        UUID id,
        String titulo,
        String conteudo,
        TipoPublicacao tipo,
        String imagemUrl,
        List<PublicacaoMidiaDTO> midias,
        UUID avaliacaoId,
        Integer avaliacaoNota,
        String avaliacaoComentario,
        boolean publicado,
        LocalDateTime publicadoEm,
        LocalDateTime createdAt
) {
    public static PublicacaoResponseDTO from(Publicacao p) {
        List<PublicacaoMidiaDTO> midias = p.getMidias() == null
                ? List.of()
                : p.getMidias().stream().map(PublicacaoMidiaDTO::from).toList();
        return new PublicacaoResponseDTO(
                p.getId(),
                p.getTitulo(),
                p.getConteudo(),
                p.getTipo(),
                p.getImagemUrl(),
                midias,
                p.getAvaliacao() != null ? p.getAvaliacao().getId() : null,
                p.getAvaliacao() != null ? p.getAvaliacao().getNota() : null,
                p.getAvaliacao() != null ? p.getAvaliacao().getComentario() : null,
                Boolean.TRUE.equals(p.getPublicado()),
                p.getPublicadoEm(),
                p.getCreatedAt()
        );
    }
}
