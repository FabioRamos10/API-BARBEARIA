package com.example.barbearia.dto.relatorio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RelatorioPorBarbeiroDTO(
        LocalDate periodoInicio,
        LocalDate periodoFim,
        List<ItemBarbeiroDTO> itens
) {
    public record ItemBarbeiroDTO(UUID barbeiroId, String nomeBarbeiro, long quantidadeAgendamentos) {
    }
}
