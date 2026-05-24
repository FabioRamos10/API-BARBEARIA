package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "chat_mensagens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMensagem {

    @Id
    private UUID id;

    private UUID conversaId;

    @Transient
    private Conversa conversa;

    private UUID remetenteUserId;

    @Builder.Default
    private TipoConteudoMensagem tipoConteudo = TipoConteudoMensagem.TEXTO;

    private String texto;

    private String anexoPath;

    private String anexoUrl;

    private String anexoContentType;

    private String anexoNome;

    @Builder.Default
    private boolean lida = false;

    private LocalDateTime enviadaEm;
}
