package com.example.barbearia.dto;

import com.example.barbearia.domain.Servico;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponseDTO(
        UUID id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer duracaoMinutos,
        String categoria,
        Boolean ativo
) {
    public static ServicoResponseDTO from(Servico s) {
        return new ServicoResponseDTO(
                s.getId(),
                s.getNome(),
                s.getDescricao(),
                s.getPreco(),
                s.getDuracaoMinutos(),
                s.getCategoria(),
                s.getAtivo()
        );
    }
}
