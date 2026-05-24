package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "comissao_lancamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComissaoLancamento {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private UUID agendamentoId;

    @Transient
    private Agendamento agendamento;

    private UUID barbeiroId;

    @Transient
    private Barbeiro barbeiro;

    private String anoMes;

    private BigDecimal valorServico;

    private BigDecimal percentualComissao;

    private BigDecimal valorComissao;

    private LocalDateTime createdAt;
}
