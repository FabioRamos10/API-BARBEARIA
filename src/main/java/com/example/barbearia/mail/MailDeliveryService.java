package com.example.barbearia.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class MailDeliveryService {

    private final ObjectProvider<JavaMailSender> javaMailSender;

    @Value("${app.mail.from:noreply@barbearia.local}")
    private String mailFrom;

    public boolean isMailConfigured() {
        return javaMailSender.getIfAvailable() != null;
    }

    public void enviar(String destinatario, String assunto, String textoPlano, String html) throws Exception {
        JavaMailSender sender = javaMailSender.getIfAvailable();
        if (sender == null || !StringUtils.hasText(destinatario)) {
            throw new IllegalStateException("JavaMailSender indisponível ou destinatário vazio");
        }
        MimeMessage mimeMessage = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(destinatario.trim());
        helper.setSubject(assunto);
        helper.setText(textoPlano, false);
        helper.setText(html, true);
        sender.send(mimeMessage);
    }
}
