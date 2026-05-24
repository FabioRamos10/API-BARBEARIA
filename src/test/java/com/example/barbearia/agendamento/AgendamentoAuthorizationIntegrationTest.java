package com.example.barbearia.agendamento;

import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.Cliente;
import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.Servico;
import com.example.barbearia.domain.User;
import com.example.barbearia.repository.BarbeiroRepository;
import com.example.barbearia.repository.ClienteRepository;
import com.example.barbearia.repository.ServicoRepository;
import com.example.barbearia.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.task.scheduling.enabled=false",
                "app.seed.admin.enabled=false"
        }
)
class AgendamentoAuthorizationIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ClienteRepository clienteRepository;

    @Autowired
    BarbeiroRepository barbeiroRepository;

    @Autowired
    ServicoRepository servicoRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    String base;
    HttpHeaders json;

    UUID clienteAId;
    UUID clienteBId;
    UUID barbeiroId;
    UUID servicoId;
    String tokenClienteA;
    String tokenClienteB;
    String tokenBarbeiro;

    @BeforeEach
    void setUp() {
        base = "http://localhost:" + port;
        json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User userA = userRepository.save(User.builder()
                .nome("Cliente A")
                .email("cliente_a_" + suffix + "@test.com")
                .senha(passwordEncoder.encode("Senha12345"))
                .role(Role.CLIENTE)
                .ativo(true)
                .build());
        Cliente clienteA = clienteRepository.save(Cliente.builder()
                .nome("Cliente A")
                .email(userA.getEmail())
                .user(userA)
                .ativo(true)
                .build());
        clienteAId = clienteA.getId();
        tokenClienteA = login(userA.getEmail());

        User userB = userRepository.save(User.builder()
                .nome("Cliente B")
                .email("cliente_b_" + suffix + "@test.com")
                .senha(passwordEncoder.encode("Senha12345"))
                .role(Role.CLIENTE)
                .ativo(true)
                .build());
        Cliente clienteB = clienteRepository.save(Cliente.builder()
                .nome("Cliente B")
                .email(userB.getEmail())
                .user(userB)
                .ativo(true)
                .build());
        clienteBId = clienteB.getId();
        tokenClienteB = login(userB.getEmail());

        User userBarbeiro = userRepository.save(User.builder()
                .nome("Barbeiro Test")
                .email("barbeiro_" + suffix + "@test.com")
                .senha(passwordEncoder.encode("Senha12345"))
                .role(Role.BARBEIRO)
                .ativo(true)
                .build());
        Barbeiro barbeiro = barbeiroRepository.save(Barbeiro.builder()
                .nome("Barbeiro Test")
                .user(userBarbeiro)
                .ativo(true)
                .build());
        barbeiroId = barbeiro.getId();
        tokenBarbeiro = login(userBarbeiro.getEmail());

        Servico servico = servicoRepository.save(Servico.builder()
                .nome("Corte")
                .preco(BigDecimal.valueOf(50))
                .duracaoMinutos(30)
                .ativo(true)
                .build());
        servicoId = servico.getId();
    }

    @Test
    void clienteNaoPodeAgendarParaOutroCliente() {
        ResponseEntity<Map> response = postAgendamento(tokenClienteA, clienteBId);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void clienteNaoPodeListarTodosAgendamentos() {
        HttpHeaders headers = authHeaders(tokenClienteA);
        ResponseEntity<String> response = restTemplate.exchange(
                base + "/agendamentos",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void barbeiroNaoPodeCriarAgendamento() {
        ResponseEntity<Map> response = postAgendamento(tokenBarbeiro, clienteAId);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void clienteAgendaParaSiMesmoComSucesso() {
        ResponseEntity<Map> response = postAgendamento(tokenClienteA, clienteAId);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void clienteBNaoConsultaAgendamentosDoClienteA() {
        ResponseEntity<String> response = restTemplate.exchange(
                base + "/agendamentos/cliente/" + clienteAId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenClienteB)),
                String.class
        );
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    private ResponseEntity<Map> postAgendamento(String token, UUID clienteId) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Map<String, Object> body = Map.of(
                "clienteId", clienteId.toString(),
                "barbeiroId", barbeiroId.toString(),
                "servicoId", servicoId.toString(),
                "inicio", inicio.toString()
        );
        return restTemplate.exchange(
                base + "/agendamentos",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)),
                Map.class
        );
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private String login(String email) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                base + "/auth/login",
                new HttpEntity<>(Map.of("email", email, "senha", "Senha12345"), json),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return (String) response.getBody().get("token");
    }
}
