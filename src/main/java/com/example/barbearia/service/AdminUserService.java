package com.example.barbearia.service;

import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.Role;
import com.example.barbearia.domain.User;
import com.example.barbearia.dto.StaffUserCreateDTO;
import com.example.barbearia.dto.StaffUserResponseDTO;
import com.example.barbearia.dto.StaffUserStatusDTO;
import com.example.barbearia.dto.StaffUserSummaryDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.BarbeiroRepository;
import com.example.barbearia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEmailService authEmailService;
    private final ContatoValidacaoService contatoValidacaoService;

    @Transactional
    public StaffUserResponseDTO criarUsuarioStaff(StaffUserCreateDTO dto) {
        if (dto.role() != Role.BARBEIRO && dto.role() != Role.RECEPCIONISTA) {
            throw new ApiException(
                    "Admin só pode criar usuários com perfil BARBEIRO ou RECEPCIONISTA",
                    HttpStatus.BAD_REQUEST
            );
        }
        contatoValidacaoService.validarNovoUsuarioStaff(dto.email(), dto.telefone());

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

        UUID barbeiroId = null;
        if (dto.role() == Role.BARBEIRO) {
            Barbeiro barbeiro = barbeiroRepository.save(Barbeiro.builder()
                    .nome(dto.nome())
                    .telefone(telNorm)
                    .percentualComissao(dto.percentualComissao())
                    .especialidades(dto.especialidades())
                    .user(saved)
                    .ativo(true)
                    .build());
            barbeiroId = barbeiro.getId();
        }

        try {
            authEmailService.enviarBoasVindas(saved);
        } catch (Exception ignored) {
        }

        return new StaffUserResponseDTO(
                saved.getId(),
                barbeiroId,
                saved.getNome(),
                saved.getEmail(),
                saved.getRole(),
                saved.getAtivo()
        );
    }

    @Transactional(readOnly = true)
    public List<StaffUserSummaryDTO> listarEquipe() {
        return userRepository.findByRoleIn(List.of(Role.BARBEIRO, Role.RECEPCIONISTA)).stream()
                .sorted(Comparator.comparing(User::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public StaffUserSummaryDTO alterarStatus(UUID userId, StaffUserStatusDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        if (user.getRole() != Role.BARBEIRO && user.getRole() != Role.RECEPCIONISTA) {
            throw new ApiException("Só é possível gerenciar barbeiros e recepcionistas", HttpStatus.BAD_REQUEST);
        }

        user.setAtivo(dto.ativo());
        userRepository.save(user);

        barbeiroRepository.findByUser_Id(userId).ifPresent(barbeiro -> {
            barbeiro.setAtivo(dto.ativo());
            barbeiroRepository.save(barbeiro);
        });

        return toSummary(user);
    }

    private StaffUserSummaryDTO toSummary(User user) {
        var barbeiroOpt = barbeiroRepository.findByUser_Id(user.getId());
        return new StaffUserSummaryDTO(
                user.getId(),
                barbeiroOpt.map(Barbeiro::getId).orElse(null),
                user.getNome(),
                user.getEmail(),
                barbeiroOpt.map(Barbeiro::getTelefone).orElse(null),
                user.getRole(),
                Boolean.TRUE.equals(user.getAtivo())
        );
    }
}
