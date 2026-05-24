package com.example.barbearia.dto.comissao;

import com.example.barbearia.domain.StatusFolhaComissao;
import jakarta.validation.constraints.NotNull;

public record FolhaComissaoStatusDTO(
        @NotNull StatusFolhaComissao status
) {
}
