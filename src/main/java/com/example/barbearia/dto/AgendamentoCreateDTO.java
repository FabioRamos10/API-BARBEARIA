package com.example.barbearia.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoCreateDTO {

    @NotNull(message = "Data e hora de início são obrigatórias")
    @Future(message = "A data de início deve ser futura")
    private LocalDateTime inicio;

    @NotNull(message = "ID do cliente é obrigatório")
    private UUID clienteId;

    @NotNull(message = "ID do barbeiro é obrigatório")
    private UUID barbeiroId;

    private UUID servicoId;

    /** Um ou mais serviços no mesmo agendamento. Se vazio, usa servicoId. */
    private List<UUID> servicoIds;

    private String observacoes;
}

