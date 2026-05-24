package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "pagamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private UUID agendamentoId;

    @Transient
    private Agendamento agendamento;

    private FormaPagamento formaPagamento;

    private BigDecimal valor;

    @Builder.Default
    private StatusPagamento status = StatusPagamento.PENDENTE;

    private String pixCopiaCola;

    private String pixQrCodeUrl;

    private String comprovantePath;

    private LocalDateTime comprovanteEnviadoEm;

    private LocalDateTime dataPagamento;

    private String observacao;

    private LocalDateTime createdAt;
}
