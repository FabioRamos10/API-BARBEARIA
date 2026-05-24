package com.example.barbearia.dto;

import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.domain.StatusAtraso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoResponseDTO {

    private UUID id;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private StatusAgendamento status;
    private String observacoes;
    private Integer atrasoMinutos;
    private String atrasoMotivo;
    private LocalDateTime atrasoInformadoEm;
    private StatusAtraso atrasoStatus;
    private LocalDateTime atrasoConfirmadoEm;
    private BigDecimal valorTotal;
    private LocalDateTime createdAt;

    private ClienteInfoDTO cliente;
    private BarbeiroInfoDTO barbeiro;
    private ServicoInfoDTO servico;
    private List<ServicoInfoDTO> servicos;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteInfoDTO {
        private UUID id;
        private String nome;
        private String email;
        private String telefone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BarbeiroInfoDTO {
        private UUID id;
        private String nome;
        private String telefone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicoInfoDTO {
        private UUID id;
        private String nome;
        private String descricao;
    }
}

