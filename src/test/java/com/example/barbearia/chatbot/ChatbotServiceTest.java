package com.example.barbearia.chatbot;

import com.example.barbearia.dto.chatbot.ChatbotRequestDTO;
import com.example.barbearia.service.chatbot.ChatbotService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotServiceTest {

    private final ChatbotService chatbotService = new ChatbotService();

    @Test
    void respondeSugestaoComoAgendar() {
        var r = chatbotService.responder(new ChatbotRequestDTO("Como agendar?"));
        assertThat(r.confianca()).isGreaterThanOrEqualTo(0.9);
        assertThat(r.resposta()).containsIgnoringCase("agendar");
    }

    @Test
    void respondePerguntaLivreComContexto() {
        var r = chatbotService.responder(new ChatbotRequestDTO("quanto tempo demora um corte?"));
        assertThat(r.resposta()).isNotBlank();
    }

    @Test
    void respondePerguntaAbertaSobrePix() {
        var r = chatbotService.responder(new ChatbotRequestDTO("como funciona o pagamento por pix depois do corte?"));
        assertThat(r.resposta()).containsIgnoringCase("pix");
    }

}
