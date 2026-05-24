package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "barbeiros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barbeiro {

    @Id
    private UUID id;

    private String nome;

    private String telefone;

    private BigDecimal percentualComissao;

    @Indexed(unique = true, sparse = true)
    private UUID userId;

    @Transient
    private User user;

    @Builder.Default
    private Boolean ativo = true;

    private String especialidades;
}
