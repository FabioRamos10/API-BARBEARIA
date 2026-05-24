package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "agendamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agendamento {

    @Id
    private UUID id;

    private LocalDateTime inicio;

    private LocalDateTime fim;

    private UUID clienteId;

    private UUID barbeiroId;

    private UUID servicoId;

    @Transient
    private Cliente cliente;

    @Transient
    private Barbeiro barbeiro;

    @Transient
    private Servico servico;

    @Builder.Default
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    private String observacoes;

    private Integer atrasoMinutos;

    private String atrasoMotivo;

    private LocalDateTime atrasoInformadoEm;

    private StatusAtraso atrasoStatus;

    private LocalDateTime atrasoConfirmadoEm;

    @Builder.Default
    private List<AgendamentoItem> itens = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public int duracaoTotalMinutos() {
        if (itens != null && !itens.isEmpty()) {
            return itens.stream()
                    .mapToInt(i -> i.getServico() != null ? i.getServico().getDuracaoMinutos() : 0)
                    .sum();
        }
        return servico != null ? servico.getDuracaoMinutos() : 0;
    }

    public BigDecimal valorTotalServicos() {
        if (itens != null && !itens.isEmpty()) {
            return itens.stream()
                    .map(i -> i.getServico() != null ? i.getServico().getPreco() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return servico != null ? servico.getPreco() : BigDecimal.ZERO;
    }
}
