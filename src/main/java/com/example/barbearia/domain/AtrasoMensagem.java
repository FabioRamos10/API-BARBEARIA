package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "atraso_mensagens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtrasoMensagem {

    @Id
    private UUID id;

    private UUID agendamentoId;

    @Transient
    private Agendamento agendamento;

    private UUID autorUserId;

    private String autorNome;

    private Role autorRole;

    private String texto;

    @Builder.Default
    private boolean respostaOficial = false;

    private LocalDateTime createdAt;
}
