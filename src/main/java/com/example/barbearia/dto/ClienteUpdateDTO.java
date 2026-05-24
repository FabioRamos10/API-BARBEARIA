package com.example.barbearia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClienteUpdateDTO(
        String telefone,
        String cpf,
        LocalDate dataNascimento,
        String observacoes
) {
}
