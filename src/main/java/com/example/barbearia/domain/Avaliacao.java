package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "avaliacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avaliacao {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private UUID agendamentoId;

    @Transient
    private Agendamento agendamento;

    private Integer nota;

    private String comentario;

    private LocalDateTime createdAt;
}
