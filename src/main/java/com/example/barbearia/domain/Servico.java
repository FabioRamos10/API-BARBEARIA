package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "servicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servico {

    @Id
    private UUID id;

    private String nome;

    private String descricao;

    private BigDecimal preco;

    private Integer duracaoMinutos;

    @Builder.Default
    private Boolean ativo = true;

    private String categoria;
}
