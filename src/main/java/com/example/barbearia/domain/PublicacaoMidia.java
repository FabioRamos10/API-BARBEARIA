package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacaoMidia {

    private UUID id;

    private TipoMidiaPublicacao tipo;

    private String storagePath;

    private String urlPublica;

    private String contentType;

    private String nomeArquivo;

    @Builder.Default
    private int ordem = 0;

    @Transient
    private Publicacao publicacao;
}
