package com.example.barbearia.service;

import com.example.barbearia.domain.User;
import com.example.barbearia.dto.LoginRequestDTO;
import com.example.barbearia.dto.LoginResponseDTO;
import com.example.barbearia.dto.RegisterRequestDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.UserRepository;
import com.example.barbearia.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ApiException("Email já cadastrado");
        }

        User user = User.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .role(dto.role())
                .ativo(true)
                .build();

        userRepository.save(user);
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new ApiException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(dto.senha(), user.getSenha())) {
            throw new ApiException("Usuário ou senha inválidos");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new LoginResponseDTO(token);
    }
}
