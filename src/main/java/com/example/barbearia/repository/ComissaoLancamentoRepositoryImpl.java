package com.example.barbearia.repository;

import com.example.barbearia.domain.ComissaoLancamento;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class ComissaoLancamentoRepositoryImpl implements ComissaoLancamentoRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<ComissaoLancamento> findByAnoMesComDetalhes(String anoMes) {
        Query query = Query.query(Criteria.where("anoMes").is(anoMes))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, ComissaoLancamento.class);
    }

    @Override
    public List<ComissaoLancamento> findByBarbeiroAndAnoMesComDetalhes(UUID barbeiroId, String anoMes) {
        Query query = Query.query(Criteria.where("barbeiroId").is(barbeiroId).and("anoMes").is(anoMes))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, ComissaoLancamento.class);
    }

    @Override
    public BigDecimal sumValorComissaoBetween(LocalDateTime desde, LocalDateTime ate) {
        Aggregation agg = newAggregation(
                match(Criteria.where("createdAt").gte(desde).lt(ate)),
                group().sum("valorComissao").as("total"));
        var results = mongoTemplate.aggregate(agg, "comissao_lancamentos", Document.class);
        if (results.getMappedResults().isEmpty()) {
            return BigDecimal.ZERO;
        }
        Object total = results.getMappedResults().get(0).get("total");
        if (total == null) {
            return BigDecimal.ZERO;
        }
        if (total instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(total.toString());
    }
}
