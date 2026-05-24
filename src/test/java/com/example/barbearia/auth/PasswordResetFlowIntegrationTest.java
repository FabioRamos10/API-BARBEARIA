package com.example.barbearia.auth;

import com.example.barbearia.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
class PasswordResetFlowIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void paginaEstaticaResetEApiRedefinicaoELogin() {
        String base = "http://localhost:" + port;

        ResponseEntity<String> pagina = restTemplate.getForEntity(
                base + "/redefinir-senha.html?token=" + UUID.randomUUID(),
                String.class
        );
        assertThat(pagina.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(pagina.getBody()).contains("Redefinir senha", "Nova senha");

        String email = "reset_flow_" + UUID.randomUUID() + "@example.com";
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Void> reg = restTemplate.postForEntity(
                base + "/auth/register",
                new HttpEntity<>(Map.of(
                        "nome", "Fluxo Reset",
                        "email", email,
                        "senha", "SenhaAntiga1",
                        "role", "CLIENTE"
                ), json),
                Void.class
        );
        assertThat(reg.getStatusCode().value()).isEqualTo(201);

        ResponseEntity<Void> forgot = restTemplate.postForEntity(
                base + "/auth/forgot-password",
                new HttpEntity<>(Map.of("email", email), json),
                Void.class
        );
        assertThat(forgot.getStatusCode().value()).isEqualTo(202);

        String token = passwordResetTokenRepository.findFirstByUser_EmailOrderByExpiresAtDesc(email)
                .orElseThrow()
                .getToken();

        ResponseEntity<Void> reset = restTemplate.postForEntity(
                base + "/auth/reset-password",
                new HttpEntity<>(Map.of("token", token, "novaSenha", "SenhaNova99"), json),
                Void.class
        );
        assertThat(reset.getStatusCode().value()).isEqualTo(204);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> login = restTemplate.postForEntity(
                base + "/auth/login",
                new HttpEntity<>(Map.of("email", email, "senha", "SenhaNova99"), json),
                Map.class
        );
        assertThat(login.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(login.getBody()).containsKey("token");
    }
}
