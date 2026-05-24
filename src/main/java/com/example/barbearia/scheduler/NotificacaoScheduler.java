package com.example.barbearia.scheduler;

import com.example.barbearia.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificacaoScheduler {

    private final NotificacaoService notificacaoService;

    @Scheduled(fixedDelayString = "${app.notificacoes.processar-pendentes-ms:120000}")
    public void processarFila() {
        notificacaoService.processarMensagensPendentes(25);
    }

    @Scheduled(fixedDelayString = "${app.notificacoes.lembrete-24h-ms:600000}")
    public void lembretes24h() {
        notificacaoService.dispararLembretesProximas24Horas();
    }
}
