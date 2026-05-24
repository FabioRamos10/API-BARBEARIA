package com.example.barbearia.dto;

import com.example.barbearia.domain.Cliente;

import java.time.LocalDate;
import java.util.UUID;

public record ClienteResponseDTO(
        UUID id,
        String nome,
        String email,
        String telefone,
        String cpf,
        LocalDate dataNascimento,
        String observacoes,
        Boolean ativo
) {
    public static ClienteResponseDTO from(Cliente c) {
        return new ClienteResponseDTO(
                c.getId(),
                c.getNome(),
                c.getEmail(),
                c.getTelefone(),
                c.getCpf(),
                c.getDataNascimento(),
                c.getObservacoes(),
                c.getAtivo()
        );
    }
}
