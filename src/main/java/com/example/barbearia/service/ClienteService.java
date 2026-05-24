package com.example.barbearia.service;

import com.example.barbearia.domain.Cliente;
import com.example.barbearia.dto.ClienteResponseDTO;
import com.example.barbearia.dto.ClienteUpdateDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ContatoValidacaoService contatoValidacaoService;

    @Transactional(readOnly = true)
    public Cliente findById(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ApiException("Cliente não encontrado", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Cliente findEntityByUserEmail(String email) {
        return clienteRepository.findByUserEmail(email)
                .orElseThrow(() -> new ApiException("Perfil de cliente não encontrado para este usuário", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO findByIdAutorizado(UUID id, Authentication authentication) {
        if (isStaff(authentication)) {
            return ClienteResponseDTO.from(findById(id));
        }
        if (hasRole(authentication, "ROLE_CLIENTE")) {
            Cliente me = findEntityByUserEmail(authentication.getName());
            if (!me.getId().equals(id)) {
                throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
            }
            return ClienteResponseDTO.from(me);
        }
        throw new ApiException("Acesso negado", HttpStatus.FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO getMe(String email) {
        return ClienteResponseDTO.from(findEntityByUserEmail(email));
    }

    @Transactional
    public ClienteResponseDTO updateMe(String email, ClienteUpdateDTO dto) {
        Cliente cliente = findEntityByUserEmail(email);
        if (dto.telefone() != null) {
            contatoValidacaoService.validarTelefoneDisponivel(dto.telefone(), cliente.getId(), null);
            cliente.setTelefone(contatoValidacaoService.normalizarTelefone(dto.telefone()));
        }
        if (dto.cpf() != null) {
            contatoValidacaoService.validarCpfDisponivel(dto.cpf(), cliente.getId());
            cliente.setCpf(dto.cpf().replaceAll("\\D", ""));
        }
        if (dto.dataNascimento() != null) {
            cliente.setDataNascimento(dto.dataNascimento());
        }
        if (dto.observacoes() != null) {
            cliente.setObservacoes(dto.observacoes());
        }
        return ClienteResponseDTO.from(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> findAllDtos() {
        return clienteRepository.findAll().stream()
                .map(ClienteResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Cliente findByEmail(String email) {
        return clienteRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Cliente não encontrado", HttpStatus.NOT_FOUND));
    }

    private static boolean isStaff(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_RECEPCIONISTA");
    }

    private static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
