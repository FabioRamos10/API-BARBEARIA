package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "publicacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publicacao {

    @Id
    private UUID id;

    private String titulo;

    private String conteudo;

    private TipoPublicacao tipo;

    private String imagemUrl;

    @Builder.Default
    private List<PublicacaoMidia> midias = new ArrayList<>();

    private UUID avaliacaoId;

    @Transient
    private Avaliacao avaliacao;

    @Builder.Default
    private Boolean publicado = true;

    private LocalDateTime publicadoEm;

    private UUID autorUserId;

    private LocalDateTime createdAt;
}
