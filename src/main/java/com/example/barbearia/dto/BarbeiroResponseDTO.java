package com.example.barbearia.dto;

import com.example.barbearia.domain.Barbeiro;

import java.math.BigDecimal;
import java.util.UUID;

public record BarbeiroResponseDTO(
        UUID id,
        String nome,
        String emailUsuario,
        String telefone,
        BigDecimal percentualComissao,
        String especialidades,
        Boolean ativo
) {
    public static BarbeiroResponseDTO from(Barbeiro b) {
        String email = b.getUser() != null ? b.getUser().getEmail() : null;
        return new BarbeiroResponseDTO(
                b.getId(),
                b.getNome(),
                email,
                b.getTelefone(),
                b.getPercentualComissao(),
                b.getEspecialidades(),
                b.getAtivo()
        );
    }
}
