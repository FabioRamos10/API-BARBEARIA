package com.example.barbearia.repository;

import com.example.barbearia.domain.Conversa;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ConversaRepositoryImpl implements ConversaRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Conversa> findByParticipante(UUID userId) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("usuarioMenorId").is(userId),
                Criteria.where("usuarioMaiorId").is(userId)));
        query.with(Sort.by(
                Sort.Order.desc("ultimaMensagemEm"),
                Sort.Order.desc("createdAt")));
        return mongoTemplate.find(query, Conversa.class);
    }
}
