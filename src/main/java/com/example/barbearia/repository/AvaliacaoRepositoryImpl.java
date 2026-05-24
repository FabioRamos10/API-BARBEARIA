package com.example.barbearia.repository;

import com.example.barbearia.domain.Avaliacao;
import com.example.barbearia.domain.Barbeiro;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class AvaliacaoRepositoryImpl implements AvaliacaoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Double calcularMediaAvaliacoesPorBarbeiro(UUID barbeiroId) {
        Aggregation agg = newAggregation(
                LookupOperation.newLookup().from("agendamentos").localField("agendamentoId").foreignField("_id").as("agendamento"),
                unwind("agendamento"),
                match(Criteria.where("agendamento.barbeiroId").is(barbeiroId)),
                group().avg("nota").as("media"));
        var results = mongoTemplate.aggregate(agg, "avaliacoes", Document.class);
        if (results.getMappedResults().isEmpty()) {
            return null;
        }
        Number media = results.getMappedResults().get(0).get("media", Number.class);
        return media != null ? media.doubleValue() : null;
    }

    @Override
    public List<Avaliacao> findByBarbeiro(Barbeiro barbeiro) {
        Aggregation agg = newAggregation(
                LookupOperation.newLookup().from("agendamentos").localField("agendamentoId").foreignField("_id").as("agendamento"),
                unwind("agendamento"),
                match(Criteria.where("agendamento.barbeiroId").is(barbeiro.getId())),
                sort(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.aggregate(agg, "avaliacoes", Avaliacao.class).getMappedResults();
    }
}
