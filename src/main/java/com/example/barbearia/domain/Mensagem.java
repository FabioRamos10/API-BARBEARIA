package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "mensagens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensagem {

    @Id
    private UUID id;

    private UUID agendamentoId;

    @Transient
    private Agendamento agendamento;

    private TipoMensagem tipo;

    @Builder.Default
    private StatusMensagem status = StatusMensagem.PENDENTE;

    private String conteudo;

    private String destinatario;

    private LocalDateTime enviadoEm;

    private String erro;

    private LocalDateTime createdAt;
}
