package com.example.barbearia.config;

import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.User;
import com.example.barbearia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminSeedRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.email:streetbarberold@gmail.com}")
    private String adminEmail;

    @Value("${app.seed.admin.password:}")
    private String adminPassword;

    @Value("${app.seed.admin.nome:Administrador}")
    private String adminNome;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminPassword)) {
            log.warn("Seed do admin ignorado: defina app.seed.admin.password (ex.: em application-local.properties)");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        userRepository.save(User.builder()
                .nome(adminNome)
                .email(adminEmail.trim())
                .senha(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .ativo(true)
                .build());
        log.info("Usuário admin padrão criado: {}", adminEmail);
    }
}
