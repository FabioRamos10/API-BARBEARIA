package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "folhas_comissao_barbeiro")
@CompoundIndex(name = "barbeiro_ano_mes", def = "{'barbeiroId': 1, 'anoMes': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolhaComissaoBarbeiro {

    @Id
    private UUID id;

    private UUID barbeiroId;

    @Transient
    private Barbeiro barbeiro;

    private String anoMes;

    @Builder.Default
    private StatusFolhaComissao status = StatusFolhaComissao.A_PAGAR;

    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Builder.Default
    private int quantidadeAtendimentos = 0;

    private LocalDateTime pagoEm;

    private LocalDateTime updatedAt;
}
