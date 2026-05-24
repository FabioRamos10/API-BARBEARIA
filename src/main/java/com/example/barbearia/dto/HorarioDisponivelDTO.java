package com.example.barbearia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponivelDTO {

    private LocalDateTime inicio;
    private LocalDateTime fim;
}
