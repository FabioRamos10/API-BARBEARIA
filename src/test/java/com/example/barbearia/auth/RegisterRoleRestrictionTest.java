package com.example.barbearia.auth;

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
class RegisterRoleRestrictionTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void registerComAdminRetorna400() {
        assertRegisterRoleRejected("ADMIN");
    }

    @Test
    void registerComBarbeiroRetorna400() {
        assertRegisterRoleRejected("BARBEIRO");
    }

    @Test
    void registerComRecepcionistaRetorna400() {
        assertRegisterRoleRejected("RECEPCIONISTA");
    }

    private void assertRegisterRoleRejected(String role) {
        String base = "http://localhost:" + port;
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                base + "/auth/register",
                new HttpEntity<>(Map.of(
                        "nome", "Usuario " + role,
                        "email", role.toLowerCase() + "_" + UUID.randomUUID() + "@example.com",
                        "senha", "Senha12345",
                        "role", role
                ), json),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("message", "Cadastro público permitido apenas para perfil CLIENTE");
    }
}
