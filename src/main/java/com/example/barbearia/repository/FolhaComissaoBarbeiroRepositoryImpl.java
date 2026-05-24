package com.example.barbearia.repository;

import com.example.barbearia.domain.FolhaComissaoBarbeiro;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class FolhaComissaoBarbeiroRepositoryImpl implements FolhaComissaoBarbeiroRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<FolhaComissaoBarbeiro> findByAnoMesOrderByBarbeiroNomeAsc(String anoMes) {
        Aggregation agg = folhaComBarbeiroNome(
                match(Criteria.where("anoMes").is(anoMes)),
                sort(Sort.Direction.ASC, "barbeiro.nome"));
        return mongoTemplate.aggregate(agg, "folhas_comissao_barbeiro", FolhaComissaoBarbeiro.class).getMappedResults();
    }

    @Override
    public List<FolhaComissaoBarbeiro> findAllByOrderByAnoMesDescBarbeiroNomeAsc() {
        Aggregation agg = folhaComBarbeiroNome(
                match(new Criteria()),
                sort(Sort.by(Sort.Order.desc("anoMes"), Sort.Order.asc("barbeiro.nome"))));
        return mongoTemplate.aggregate(agg, "folhas_comissao_barbeiro", FolhaComissaoBarbeiro.class).getMappedResults();
    }

    @Override
    public List<String> findDistinctAnoMesSortedDesc() {
        return mongoTemplate.findDistinct("anoMes", FolhaComissaoBarbeiro.class, String.class)
                .stream()
                .sorted(java.util.Comparator.reverseOrder())
                .toList();
    }

    private Aggregation folhaComBarbeiroNome(
            org.springframework.data.mongodb.core.aggregation.MatchOperation matchOp,
            org.springframework.data.mongodb.core.aggregation.SortOperation sortOp) {
        return newAggregation(
                matchOp,
                LookupOperation.newLookup().from("barbeiros").localField("barbeiroId").foreignField("_id").as("barbeiro"),
                unwind("barbeiro"),
                sortOp);
    }
}
