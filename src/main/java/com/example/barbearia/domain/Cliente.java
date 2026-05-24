package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.UUID;

@Document(collection = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    private UUID id;

    private String nome;

    @Indexed(unique = true)
    private String email;

    private String telefone;

    private String cpf;

    private LocalDate dataNascimento;

    @Indexed(unique = true, sparse = true)
    private UUID userId;

    @Transient
    private User user;

    @Builder.Default
    private Boolean ativo = true;

    private String observacoes;
}
