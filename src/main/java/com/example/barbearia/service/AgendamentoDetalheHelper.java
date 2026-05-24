package com.example.barbearia.service;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.AgendamentoItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * Carrega referências @Transient do agendamento (Mongo persiste só IDs).
 */
@Component
public class AgendamentoDetalheHelper {

    private final ClienteService clienteService;
    private final BarbeiroService barbeiroService;
    private final ServicoService servicoService;
    private final UserService userService;

    public AgendamentoDetalheHelper(
            ClienteService clienteService,
            BarbeiroService barbeiroService,
            ServicoService servicoService,
            UserService userService
    ) {
        this.clienteService = clienteService;
        this.barbeiroService = barbeiroService;
        this.servicoService = servicoService;
        this.userService = userService;
    }

    public void enriquecer(Agendamento ag) {
        if (ag == null) {
            return;
        }
        if (ag.getCliente() == null && ag.getClienteId() != null) {
            ag.setCliente(clienteService.findById(ag.getClienteId()));
        }
        if (ag.getBarbeiro() == null && ag.getBarbeiroId() != null) {
            ag.setBarbeiro(barbeiroService.findById(ag.getBarbeiroId()));
        }
        if (ag.getServico() == null && ag.getServicoId() != null) {
            ag.setServico(servicoService.findById(ag.getServicoId()));
        }
        if (ag.getItens() != null) {
            for (AgendamentoItem item : ag.getItens()) {
                if (item.getServico() == null && item.getServicoId() != null) {
                    item.setServico(servicoService.findById(item.getServicoId()));
                }
            }
        }
        if (ag.getCliente() != null && ag.getCliente().getUser() == null && ag.getCliente().getUserId() != null) {
            ag.getCliente().setUser(userService.requireById(ag.getCliente().getUserId()));
        }
        if (ag.getBarbeiro() != null && ag.getBarbeiro().getUser() == null && ag.getBarbeiro().getUserId() != null) {
            ag.getBarbeiro().setUser(userService.requireById(ag.getBarbeiro().getUserId()));
        }
    }

    public String nomesServicos(Agendamento ag) {
        enriquecer(ag);
        if (ag.getItens() != null && !ag.getItens().isEmpty()) {
            String joined = ag.getItens().stream()
                    .filter(item -> item.getServico() != null)
                    .map(item -> item.getServico().getNome())
                    .collect(Collectors.joining(", "));
            if (!joined.isBlank()) {
                return joined;
            }
        }
        if (ag.getServico() != null) {
            return ag.getServico().getNome();
        }
        return "Serviços";
    }

    public String nomeCliente(Agendamento ag) {
        enriquecer(ag);
        return ag.getCliente() != null ? ag.getCliente().getNome() : "Cliente";
    }

    public String nomeBarbeiro(Agendamento ag) {
        enriquecer(ag);
        return ag.getBarbeiro() != null ? ag.getBarbeiro().getNome() : "Profissional";
    }

    /** Soma preços de todos os serviços do atendimento (inclui múltiplos itens). */
    public BigDecimal valorTotal(Agendamento ag) {
        enriquecer(ag);
        if (ag.getItens() != null && !ag.getItens().isEmpty()) {
            BigDecimal soma = BigDecimal.ZERO;
            for (AgendamentoItem item : ag.getItens()) {
                BigDecimal preco = precoDoItem(item);
                if (preco != null) {
                    soma = soma.add(preco);
                }
            }
            if (soma.compareTo(BigDecimal.ZERO) > 0) {
                return soma;
            }
        }
        if (ag.getServico() != null && ag.getServico().getPreco() != null) {
            return ag.getServico().getPreco();
        }
        if (ag.getServicoId() != null) {
            return servicoService.findById(ag.getServicoId()).getPreco();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal precoDoItem(AgendamentoItem item) {
        if (item.getServico() != null && item.getServico().getPreco() != null) {
            return item.getServico().getPreco();
        }
        if (item.getServicoId() != null) {
            try {
                return servicoService.findById(item.getServicoId()).getPreco();
            } catch (Exception ignored) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }
}
