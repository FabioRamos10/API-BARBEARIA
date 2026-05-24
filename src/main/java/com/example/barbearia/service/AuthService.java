package com.example.barbearia.service;

import com.example.barbearia.domain.Cliente;
import com.example.barbearia.domain.PasswordResetToken;
import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.User;
import com.example.barbearia.dto.ForgotPasswordRequestDTO;
import com.example.barbearia.dto.LoginRequestDTO;
import com.example.barbearia.dto.LoginResponseDTO;
import com.example.barbearia.dto.RegisterRequestDTO;
import com.example.barbearia.dto.ResetPasswordRequestDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.ClienteRepository;
import com.example.barbearia.repository.PasswordResetTokenRepository;
import com.example.barbearia.repository.UserRepository;
import com.example.barbearia.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthEmailService authEmailService;
    private final ContatoValidacaoService contatoValidacaoService;

    @Transactional
    public void register(RegisterRequestDTO dto) {
        if (dto.role() != Role.CLIENTE) {
            throw new ApiException(
                    "Cadastro público permitido apenas para perfil CLIENTE",
                    HttpStatus.BAD_REQUEST
            );
        }
        contatoValidacaoService.validarNovoCadastroCliente(dto.email(), dto.telefone(), null);

        String emailNorm = contatoValidacaoService.normalizarEmail(dto.email());
        String telNorm = contatoValidacaoService.normalizarTelefone(dto.telefone());

        User user = User.builder()
                .nome(dto.nome())
                .email(emailNorm)
                .senha(passwordEncoder.encode(dto.senha()))
                .role(dto.role())
                .ativo(true)
                .build();

        User saved = userRepository.save(user);

        clienteRepository.save(Cliente.builder()
                .nome(dto.nome())
                .email(emailNorm)
                .telefone(telNorm)
                .user(saved)
                .ativo(true)
                .build());

        try {
            authEmailService.enviarBoasVindas(saved);
        } catch (Exception ignored) {
        }
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

    /**
     * Sempre conclui sem erro para não revelar se o e-mail existe.
     */
    @Transactional
    public void solicitarRecuperacaoSenha(ForgotPasswordRequestDTO dto) {
        userRepository.findByEmail(dto.email()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);
            String token = UUID.randomUUID().toString();
            PasswordResetToken entity = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(entity);
            authEmailService.enviarRecuperacaoSenha(user, token);
        });
    }

    @Transactional
    public void redefinirSenha(ResetPasswordRequestDTO dto) {
        PasswordResetToken prt = passwordResetTokenRepository.findByTokenAndUsedFalse(dto.token())
                .orElseThrow(() -> new ApiException("Token inválido ou já utilizado", HttpStatus.BAD_REQUEST));
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException("Token expirado. Solicite uma nova recuperação de senha.", HttpStatus.BAD_REQUEST);
        }
        User user = prt.getUser();
        user.setSenha(passwordEncoder.encode(dto.novaSenha()));
        userRepository.save(user);
        passwordResetTokenRepository.deleteByUser(user);
    }
}
