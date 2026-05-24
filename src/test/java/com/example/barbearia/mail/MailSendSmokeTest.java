package com.example.barbearia.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.ActiveProfiles;

import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "mail.smoke", matches = "true")
class MailSendSmokeTest {

    @Autowired
    private ObjectProvider<JavaMailSender> javaMailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Test
    void enviaEmailDeTeste() throws Exception {
        JavaMailSender sender = javaMailSender.getIfAvailable();
        assertThat(sender)
                .as("JavaMailSender ausente: confira application-local.properties e perfil local")
                .isNotNull();

        MimeMessage mime = sender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(mime, true, "UTF-8");
        h.setFrom(mailFrom);
        h.setTo(mailUsername);
        h.setSubject("Barbearia API — teste SMTP");
        String plain = "Se você recebeu este e-mail, o SMTP com Gmail está funcionando.";
        String html = """
                <html><body style="margin:0;background:#050508;color:#eafff0;font-family:system-ui,sans-serif;padding:24px;">
                <p style="color:#39ff14;font-weight:600;">Barbearia</p>
                <p>%s</p></body></html>
                """.formatted(plain);
        h.setText(plain, false);
        h.setText(html, true);
        sender.send(mime);
    }
}
