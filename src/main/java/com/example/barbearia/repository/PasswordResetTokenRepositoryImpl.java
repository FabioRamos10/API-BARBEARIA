package com.example.barbearia.repository;

import com.example.barbearia.domain.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepositoryCustom {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<PasswordResetToken> findFirstByUser_EmailOrderByExpiresAtDesc(String email) {
        return userRepository.findByEmail(email).flatMap(user -> {
            Query query = Query.query(Criteria.where("userId").is(user.getId()))
                    .with(Sort.by(Sort.Direction.DESC, "expiresAt"));
            return Optional.ofNullable(mongoTemplate.findOne(query, PasswordResetToken.class));
        });
    }
}
