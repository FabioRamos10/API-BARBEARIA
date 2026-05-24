package com.example.barbearia.dto;

import com.example.barbearia.domain.ChatMensagem;
import com.example.barbearia.domain.TipoConteudoMensagem;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMensagemResponseDTO(
        UUID id,
        UUID conversaId,
        UUID remetenteUserId,
        String remetenteNome,
        TipoConteudoMensagem tipoConteudo,
        String texto,
        String anexoUrl,
        String anexoContentType,
        String anexoNome,
        boolean lida,
        LocalDateTime enviadaEm
) {
    public static ChatMensagemResponseDTO from(ChatMensagem m, String remetenteNome) {
        UUID conversaId = m.getConversaId();
        if (conversaId == null && m.getConversa() != null) {
            conversaId = m.getConversa().getId();
        }
        return new ChatMensagemResponseDTO(
                m.getId(),
                conversaId,
                m.getRemetenteUserId(),
                remetenteNome,
                m.getTipoConteudo(),
                m.getTexto(),
                m.getAnexoUrl(),
                m.getAnexoContentType(),
                m.getAnexoNome(),
                m.isLida(),
                m.getEnviadaEm()
        );
    }
}
