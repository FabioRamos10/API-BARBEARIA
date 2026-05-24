package com.example.barbearia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaDisponivelResponseDTO {

    private UUID barbeiroId;
    private LocalDate data;
    private int duracaoTotalMinutos;
    private LocalTime abertura;
    private LocalTime fechamento;
    private int intervaloMinutos;
    private List<HorarioDisponivelDTO> horariosDisponiveis;
}
