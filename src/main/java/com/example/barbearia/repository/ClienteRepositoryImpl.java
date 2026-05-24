package com.example.barbearia.repository;

import com.example.barbearia.domain.Cliente;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClienteRepositoryImpl implements ClienteRepositoryCustom {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Cliente> findByUserEmail(String email) {
        return userRepository.findByEmail(email)
                .flatMap(user -> Optional.ofNullable(
                        mongoTemplate.findOne(
                                Query.query(Criteria.where("userId").is(user.getId())),
                                Cliente.class)));
    }
}
