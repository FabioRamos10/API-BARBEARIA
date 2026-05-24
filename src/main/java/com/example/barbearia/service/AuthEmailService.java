package com.example.barbearia.service;

import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.User;
import com.example.barbearia.mail.EmailTemplateEngine;
import com.example.barbearia.mail.MailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthEmailService {

    private final MailDeliveryService mailDeliveryService;
    private final EmailTemplateEngine emailTemplateEngine;

    @Value("${app.mail.password-reset-base-url:}")
    private String passwordResetBaseUrl;

    public void enviarBoasVindas(User user) {
        if (!mailDeliveryService.isMailConfigured() || !StringUtils.hasText(user.getEmail())) {
            return;
        }
        try {
            String papel = nomePapel(user.getRole());
            String html = emailTemplateEngine.renderBoasVindas(user.getNome(), papel);
            String plain = "Olá, " + user.getNome() + "! Sua conta foi criada como " + papel + ". Bem-vindo à barbearia.";
            mailDeliveryService.enviar(user.getEmail(), "Bem-vindo — Barbearia", plain, html);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de boas-vindas: {}", e.getMessage());
        }
    }

    public void enviarRecuperacaoSenha(User user, String tokenPuro) {
        if (!mailDeliveryService.isMailConfigured() || !StringUtils.hasText(user.getEmail())) {
            return;
        }
        try {
            String link = montarLinkReset(tokenPuro);
            String html = emailTemplateEngine.renderRecuperacaoSenha(user.getNome(), link, tokenPuro);
            String plain = montarTextoPlanoReset(user.getNome(), link, tokenPuro);
            mailDeliveryService.enviar(user.getEmail(), "Redefinição de senha — Barbearia", plain, html);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de recuperação: {}", e.getMessage());
        }
    }

    private String montarLinkReset(String token) {
        if (!StringUtils.hasText(passwordResetBaseUrl)) {
            return "";
        }
        String enc = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String base = passwordResetBaseUrl.trim();
        if (base.contains("?")) {
            return base + "&token=" + enc;
        }
        return base + "?token=" + enc;
    }

    private String montarTextoPlanoReset(String nome, String link, String token) {
        if (StringUtils.hasText(link)) {
            return "Olá, " + nome + ". Para redefinir sua senha, acesse: " + link + " (válido por tempo limitado).";
        }
        return "Olá, " + nome + ". Use o token abaixo na API POST /auth/reset-password junto com sua nova senha: " + token;
    }

    private static String nomePapel(Role role) {
        if (role == null) {
            return "Usuário";
        }
        return switch (role) {
            case ADMIN -> "Administrador";
            case BARBEIRO -> "Barbeiro";
            case RECEPCIONISTA -> "Recepcionista";
            case CLIENTE -> "Cliente";
        };
    }
}
