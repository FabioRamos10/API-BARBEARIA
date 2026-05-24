package com.example.barbearia.dto;

import com.example.barbearia.domain.TipoPublicacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PublicacaoCreateDTO(
        @NotBlank String titulo,
        @NotBlank String conteudo,
        @NotNull TipoPublicacao tipo,
        String imagemUrl,
        UUID avaliacaoId,
        Boolean publicado
) {
}
