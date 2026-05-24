package com.example.barbearia.repository;

import com.example.barbearia.domain.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepositoryCustom {

    Optional<PasswordResetToken> findFirstByUser_EmailOrderByExpiresAtDesc(String email);
}
