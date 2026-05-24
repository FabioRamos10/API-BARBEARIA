package com.example.barbearia.controller;

import com.example.barbearia.dto.chatbot.ChatbotRequestDTO;
import com.example.barbearia.dto.chatbot.ChatbotResponseDTO;
import com.example.barbearia.service.chatbot.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/mensagem")
    public ResponseEntity<ChatbotResponseDTO> mensagem(@RequestBody @Valid ChatbotRequestDTO dto) {
        return ResponseEntity.ok(chatbotService.responder(dto));
    }
}
