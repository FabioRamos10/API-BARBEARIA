package com.example.barbearia.auth;

import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.User;
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
class AdminStaffUserIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    String base;
    String adminToken;

    @BeforeEach
    void setUp() {
        base = "http://localhost:" + port;
        String email = "admin_staff_" + UUID.randomUUID() + "@test.com";
        userRepository.save(User.builder()
                .nome("Admin Test")
                .email(email)
                .senha(passwordEncoder.encode("Admin12345"))
                .role(Role.ADMIN)
                .ativo(true)
                .build());
        adminToken = login(email, "Admin12345");
    }

    @Test
    void adminCriaRecepcionista() {
        ResponseEntity<Map> response = criarStaff("RECEPCIONISTA");
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).containsEntry("role", "RECEPCIONISTA");
    }

    @Test
    void adminCriaBarbeiro() {
        ResponseEntity<Map> response = criarStaff("BARBEIRO");
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).containsEntry("role", "BARBEIRO");
        assertThat(response.getBody().get("barbeiroId")).isNotNull();
    }

    @Test
    void clienteNaoCriaStaff() {
        String clienteEmail = "cli_" + UUID.randomUUID() + "@test.com";
        restTemplate.postForEntity(
                base + "/auth/register",
                new HttpEntity<>(Map.of(
                        "nome", "Cliente",
                        "email", clienteEmail,
                        "senha", "Senha12345",
                        "role", "CLIENTE"
                ), jsonHeaders()),
                Void.class
        );
        String clienteToken = login(clienteEmail, "Senha12345");

        ResponseEntity<String> response = restTemplate.exchange(
                base + "/admin/usuarios",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "nome", "Barbeiro X",
                        "email", "barbeiro_" + UUID.randomUUID() + "@test.com",
                        "senha", "Senha12345",
                        "role", "BARBEIRO"
                ), authHeaders(clienteToken)),
                String.class
        );
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    private ResponseEntity<Map> criarStaff(String role) {
        return restTemplate.exchange(
                base + "/admin/usuarios",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "nome", "Staff " + role,
                        "email", role.toLowerCase() + "_" + UUID.randomUUID() + "@test.com",
                        "senha", "Senha12345",
                        "role", role,
                        "telefone", "62999990000"
                ), authHeaders(adminToken)),
                Map.class
        );
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private String login(String email, String senha) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                base + "/auth/login",
                new HttpEntity<>(Map.of("email", email, "senha", senha), jsonHeaders()),
                Map.class
        );
        return (String) response.getBody().get("token");
    }
}
