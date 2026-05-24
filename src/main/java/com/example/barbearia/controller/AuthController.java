package com.example.barbearia.controller;

import com.example.barbearia.dto.ForgotPasswordRequestDTO;
import com.example.barbearia.dto.LoginRequestDTO;
import com.example.barbearia.dto.LoginResponseDTO;
import com.example.barbearia.dto.RegisterRequestDTO;
import com.example.barbearia.dto.ResetPasswordRequestDTO;
import com.example.barbearia.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequestDTO dto) {
        authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO dto) {
        authService.solicitarRecuperacaoSenha(dto);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO dto) {
        authService.redefinirSenha(dto);
        return ResponseEntity.noContent().build();
    }
}
