package com.example.barbearia.service;

import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.BarbeiroRepository;
import com.example.barbearia.repository.ClienteRepository;
import com.example.barbearia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ContatoValidacaoService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final UserRepository userRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;

    public String normalizarEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    public String normalizarTelefone(String telefone) {
        if (!StringUtils.hasText(telefone)) {
            return null;
        }
        String digits = telefone.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 11) {
            throw new ApiException("Telefone inválido. Use DDD + número (10 ou 11 dígitos).", HttpStatus.BAD_REQUEST);
        }
        return digits;
    }

    public void validarFormatoEmail(String email) {
        if (!StringUtils.hasText(email) || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ApiException("E-mail inválido", HttpStatus.BAD_REQUEST);
        }
    }

    public void validarEmailDisponivel(String email, UUID userIdExcluir) {
        validarFormatoEmail(email);
        String norm = normalizarEmail(email);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(norm, userIdExcluir)) {
            throw new ApiException("Este e-mail já está cadastrado no sistema", HttpStatus.CONFLICT);
        }
        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(norm, userIdExcluir)) {
            throw new ApiException("Este e-mail já está cadastrado para um cliente", HttpStatus.CONFLICT);
        }
    }

    public void validarTelefoneDisponivel(String telefone, UUID clienteIdExcluir, UUID barbeiroIdExcluir) {
        String norm = normalizarTelefone(telefone);
        if (norm == null) {
            return;
        }
        if (clienteRepository.existsByTelefoneAndIdNot(norm, clienteIdExcluir)) {
            throw new ApiException("Este telefone já está cadastrado para outro cliente", HttpStatus.CONFLICT);
        }
        if (barbeiroRepository.existsByTelefoneAndIdNot(norm, barbeiroIdExcluir)) {
            throw new ApiException("Este telefone já está cadastrado para outro barbeiro", HttpStatus.CONFLICT);
        }
    }

    public void validarCpfDisponivel(String cpf, UUID clienteIdExcluir) {
        if (!StringUtils.hasText(cpf)) {
            return;
        }
        String norm = cpf.replaceAll("\\D", "");
        if (norm.length() != 11) {
            throw new ApiException("CPF inválido", HttpStatus.BAD_REQUEST);
        }
        if (clienteRepository.existsByCpfAndIdNot(norm, clienteIdExcluir)) {
            throw new ApiException("Este CPF já está cadastrado", HttpStatus.CONFLICT);
        }
    }

    public void validarNovoCadastroCliente(String email, String telefone, String cpf) {
        validarEmailDisponivel(email, null);
        validarTelefoneDisponivel(telefone, null, null);
        validarCpfDisponivel(cpf, null);
    }

    public void validarNovoUsuarioStaff(String email, String telefone) {
        validarEmailDisponivel(email, null);
        validarTelefoneDisponivel(telefone, null, null);
    }
}
