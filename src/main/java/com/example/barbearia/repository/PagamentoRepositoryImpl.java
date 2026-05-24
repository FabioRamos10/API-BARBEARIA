package com.example.barbearia.repository;

import com.example.barbearia.domain.Pagamento;
import com.example.barbearia.domain.StatusPagamento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class PagamentoRepositoryImpl implements PagamentoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public long countByAgendamentoInicioBetweenAndStatus(
            LocalDateTime desde,
            LocalDateTime ate,
            StatusPagamento status) {
        Aggregation agg = newAggregation(
                match(Criteria.where("status").is(status)),
                LookupOperation.newLookup().from("agendamentos").localField("agendamentoId").foreignField("_id").as("agendamento"),
                unwind("agendamento", true),
                match(Criteria.where("agendamento.inicio").gte(desde).lt(ate)),
                group().count().as("total"));
        var result = mongoTemplate.aggregate(agg, "pagamentos", org.bson.Document.class);
        if (result.getMappedResults().isEmpty()) {
            return 0L;
        }
        return result.getMappedResults().get(0).get("total", Number.class).longValue();
    }
}
