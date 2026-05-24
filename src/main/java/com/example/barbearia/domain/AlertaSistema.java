package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "alertas_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaSistema {

    @Id
    private UUID id;

    private UUID destinatarioUserId;

    private String titulo;

    private String mensagem;

    private TipoAlerta tipo;

    private UUID referenciaId;

    @Builder.Default
    private boolean lido = false;

    private LocalDateTime createdAt;
}
