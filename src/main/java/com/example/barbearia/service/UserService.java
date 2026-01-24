package com.example.barbearia.service;

import com.example.barbearia.domain.User;
import com.example.barbearia.dto.UserCreateDTO;
import com.example.barbearia.exception.ApiException;
import com.example.barbearia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public User create(UserCreateDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new ApiException("E-mail já cadastrado");
        }

        User user = User.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .role(dto.role())
                .ativo(true)
                .build();

        return userRepository.save(user);
    }
}
