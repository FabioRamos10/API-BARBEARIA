package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Transient;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoItem {

    private UUID id;

    private UUID servicoId;

    @Builder.Default
    private int ordem = 0;

    @Transient
    private Servico servico;

    @Transient
    private Agendamento agendamento;
}
