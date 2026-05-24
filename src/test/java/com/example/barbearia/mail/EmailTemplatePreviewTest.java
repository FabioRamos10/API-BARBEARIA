package com.example.barbearia.mail;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.Cliente;
import com.example.barbearia.domain.Mensagem;
import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.Servico;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.domain.StatusMensagem;
import com.example.barbearia.domain.TipoMensagem;
import com.example.barbearia.domain.User;
import com.example.barbearia.repository.AgendamentoRepository;
import com.example.barbearia.repository.BarbeiroRepository;
import com.example.barbearia.repository.ClienteRepository;
import com.example.barbearia.repository.ServicoRepository;
import com.example.barbearia.repository.UserRepository;
import com.example.barbearia.service.NotificacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gera arquivos HTML em {@code target/email-previews/} para visualizar todos os templates (sem enviar e-mail).
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class EmailTemplatePreviewTest {

    @Autowired
    private EmailTemplateEngine emailTemplateEngine;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private BarbeiroRepository barbeiroRepository;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void exportarTemplatesHtml() throws Exception {
        Path dir = Path.of("target", "email-previews");
        Files.createDirectories(dir);

        Files.writeString(dir.resolve("01-boas-vindas.html"),
                emailTemplateEngine.renderBoasVindas("Fábio", "Cliente"),
                StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("02-recuperacao-com-link.html"),
                emailTemplateEngine.renderRecuperacaoSenha(
                        "Fábio",
                        "http://localhost:4200/redefinir-senha?token=exemplo-token-uuid",
                        "exemplo-token-uuid"
                ),
                StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("03-recuperacao-so-token.html"),
                emailTemplateEngine.renderRecuperacaoSenha("Fábio", "", "exemplo-token-uuid"),
                StandardCharsets.UTF_8);

        Fixture f = criarAgendamentoDemo();

        Files.writeString(dir.resolve("04-cliente-novo-agendamento.html"),
                emailTemplateEngine.renderHtml(msg(f.ag(), NotificacaoService.PREFIX_NOVO + " texto plano."), f.ag()),
                StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("05-cliente-lembrete.html"),
                emailTemplateEngine.renderHtml(msg(f.ag(), NotificacaoService.PREFIX_LEMBRETE + " lembrete."), f.ag()),
                StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("06-cliente-cancelado.html"),
                emailTemplateEngine.renderHtml(msg(f.ag(), NotificacaoService.PREFIX_CANCELADO + " cancel."), f.ag()),
                StandardCharsets.UTF_8);

        f.ag().setStatus(StatusAgendamento.CONFIRMADO);
        Files.writeString(dir.resolve("07-cliente-status-confirmado.html"),
                emailTemplateEngine.renderHtml(msg(f.ag(), NotificacaoService.PREFIX_STATUS + StatusAgendamento.CONFIRMADO.name() + " x"), f.ag()),
                StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("08-barbeiro-copia-novo.html"),
                emailTemplateEngine.renderHtml(msg(f.ag(), NotificacaoService.PREFIX_AVISO_BARBEIRO_NOVO + " cópia."), f.ag()),
                StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("09-barbeiro-copia-cancel.html"),
                emailTemplateEngine.renderHtml(msg(f.ag(), NotificacaoService.PREFIX_AVISO_BARBEIRO_CANCEL + " cópia."), f.ag()),
                StandardCharsets.UTF_8);

        f.ag().setStatus(StatusAgendamento.EM_ANDAMENTO);
        Files.writeString(dir.resolve("10-barbeiro-copia-status.html"),
                emailTemplateEngine.renderHtml(
                        msg(f.ag(), NotificacaoService.PREFIX_AVISO_BARBEIRO_STATUS + StatusAgendamento.EM_ANDAMENTO.name()),
                        f.ag()
                ),
                StandardCharsets.UTF_8);

        assertThat(Files.list(dir)).hasSizeGreaterThanOrEqualTo(10);
    }

    private static Mensagem msg(Agendamento ag, String conteudo) {
        return Mensagem.builder()
                .agendamento(ag)
                .tipo(TipoMensagem.EMAIL)
                .status(StatusMensagem.PENDENTE)
                .conteudo(conteudo)
                .destinatario("demo@local.test")
                .build();
    }

    private Fixture criarAgendamentoDemo() {
        User uCliente = userRepository.save(User.builder()
                .nome("Cliente Demo")
                .email("cliente-demo-templates@local.test")
                .senha(passwordEncoder.encode("senha123"))
                .role(Role.CLIENTE)
                .ativo(true)
                .build());
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Demo")
                .email("cliente-demo-templates@local.test")
                .telefone("62999990001")
                .user(uCliente)
                .ativo(true)
                .build());

        User uBarbeiro = userRepository.save(User.builder()
                .nome("Barbeiro Demo")
                .email("barbeiro-demo-templates@local.test")
                .senha(passwordEncoder.encode("senha123"))
                .role(Role.BARBEIRO)
                .ativo(true)
                .build());
        Barbeiro barbeiro = barbeiroRepository.save(Barbeiro.builder()
                .nome("Barbeiro Demo")
                .telefone("62999990002")
                .user(uBarbeiro)
                .ativo(true)
                .build());

        Servico servico = servicoRepository.save(Servico.builder()
                .nome("Corte + barba")
                .descricao("Serviço demo para preview de e-mail")
                .preco(new BigDecimal("85.00"))
                .duracaoMinutos(45)
                .categoria("Combo")
                .ativo(true)
                .build());

        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Agendamento ag = agendamentoRepository.save(Agendamento.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .inicio(inicio)
                .fim(inicio.plusMinutes(45))
                .status(StatusAgendamento.AGENDADO)
                .observacoes("Preview gerado pelo teste EmailTemplatePreviewTest")
                .build());

        Agendamento carregado = agendamentoRepository.findByIdComDetalhes(ag.getId()).orElseThrow();
        return new Fixture(carregado);
    }

    private record Fixture(Agendamento ag) {
    }
}
