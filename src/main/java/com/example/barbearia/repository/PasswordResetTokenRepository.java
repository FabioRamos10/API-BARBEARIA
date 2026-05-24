package com.example.barbearia.repository;

import com.example.barbearia.domain.PasswordResetToken;
import com.example.barbearia.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, UUID>, PasswordResetTokenRepositoryCustom {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    void deleteByUserId(UUID userId);

    default void deleteByUser(User user) {
        if (user != null && user.getId() != null) {
            deleteByUserId(user.getId());
        }
    }
}
