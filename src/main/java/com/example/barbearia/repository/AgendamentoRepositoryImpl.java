package com.example.barbearia.repository;

import com.example.barbearia.domain.Agendamento;
import com.example.barbearia.domain.Barbeiro;
import com.example.barbearia.domain.StatusAgendamento;
import com.example.barbearia.service.AgendamentoDetalheHelper;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class AgendamentoRepositoryImpl implements AgendamentoRepositoryCustom {

    private final MongoTemplate mongoTemplate;
    private final AgendamentoDetalheHelper agendamentoDetalheHelper;

    @Override
    public List<Agendamento> findByBarbeiroAndPeriodo(
            Barbeiro barbeiro,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<StatusAgendamento> statuses) {
        Query query = new Query(Criteria.where("barbeiroId").is(barbeiro.getId())
                .and("inicio").gte(inicio)
                .and("fim").lte(fim)
                .and("status").in(statuses));
        return mongoTemplate.find(query, Agendamento.class);
    }

    @Override
    public boolean existsConflitoHorario(
            UUID barbeiroId,
            LocalDateTime inicio,
            LocalDateTime fim,
            List<StatusAgendamento> statuses) {
        Query query = new Query(Criteria.where("barbeiroId").is(barbeiroId)
                .and("status").in(statuses)
                .and("inicio").lt(fim)
                .and("fim").gt(inicio));
        return mongoTemplate.exists(query, Agendamento.class);
    }

    @Override
    public List<Agendamento> findAgendamentosParaLembrete(
            LocalDateTime inicio,
            LocalDateTime fim,
            List<StatusAgendamento> statuses) {
        return findParaLembreteNaJanela(statuses, inicio, fim);
    }

    @Override
    public List<Object[]> countGroupedByStatusBetween(LocalDateTime desde, LocalDateTime ate) {
        Aggregation agg = newAggregation(
                match(Criteria.where("inicio").gte(desde).lt(ate)),
                group("status").count().as("count"),
                project("count").and("_id").as("status"));
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "agendamentos", Document.class);
        List<Object[]> rows = new ArrayList<>();
        for (Document doc : results.getMappedResults()) {
            StatusAgendamento status = parseStatus(doc.get("status"));
            if (status != null) {
                rows.add(new Object[]{status, doc.get("count", Number.class).longValue()});
            }
        }
        return rows;
    }

    @Override
    public List<Object[]> countGroupedByBarbeiroBetween(LocalDateTime desde, LocalDateTime ate) {
        Aggregation agg = newAggregation(
                match(Criteria.where("inicio").gte(desde).lt(ate)),
                group("barbeiroId").count().as("count"),
                LookupOperation.newLookup().from("barbeiros").localField("_id").foreignField("_id").as("barbeiro"),
                unwind("barbeiro", true),
                project("count")
                        .and("_id").as("barbeiroId")
                        .and("barbeiro.nome").as("nome"),
                sort(Sort.Direction.DESC, "count"));
        return mapBarbeiroCountRows(agg);
    }

    @Override
    public long countBetween(LocalDateTime desde, LocalDateTime ate) {
        Query query = new Query(Criteria.where("inicio").gte(desde).lt(ate));
        return mongoTemplate.count(query, Agendamento.class);
    }

    @Override
    public BigDecimal sumPrecoServicosConcluidosBetween(
            LocalDateTime desde,
            LocalDateTime ate,
            StatusAgendamento statusConcluido) {
        Aggregation agg = newAggregation(
                match(Criteria.where("inicio").gte(desde).lt(ate).and("status").is(statusConcluido)),
                LookupOperation.newLookup().from("servicos").localField("servicoId").foreignField("_id").as("servico"),
                unwind("servico", true),
                group().sum("servico.preco").as("total"));
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "agendamentos", Document.class);
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

    @Override
    public List<Agendamento> findParaLembreteNaJanela(
            List<StatusAgendamento> statuses,
            LocalDateTime inicio,
            LocalDateTime fim) {
        Query query = new Query(Criteria.where("status").in(statuses)
                .and("inicio").gte(inicio)
                .lt(fim));
        return mongoTemplate.find(query, Agendamento.class);
    }

    @Override
    public Optional<Agendamento> findByIdComDetalhes(UUID id) {
        Agendamento ag = mongoTemplate.findById(id, Agendamento.class);
        if (ag == null) {
            return Optional.empty();
        }
        agendamentoDetalheHelper.enriquecer(ag);
        return Optional.of(ag);
    }

    @Override
    public List<Object[]> countGroupedByServicoBetween(LocalDateTime desde, LocalDateTime ate) {
        Aggregation agg = newAggregation(
                match(Criteria.where("inicio").gte(desde).lt(ate)),
                group("servicoId").count().as("count"),
                LookupOperation.newLookup().from("servicos").localField("_id").foreignField("_id").as("servico"),
                unwind("servico", true),
                project("count")
                        .and("_id").as("servicoId")
                        .and("servico.nome").as("nome"),
                sort(Sort.Direction.DESC, "count"));
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "agendamentos", Document.class);
        List<Object[]> rows = new ArrayList<>();
        for (Document doc : results.getMappedResults()) {
            UUID servicoId = doc.get("servicoId", UUID.class);
            if (servicoId == null) {
                continue;
            }
            rows.add(new Object[]{
                    servicoId,
                    doc.getString("nome") != null ? doc.getString("nome") : "Serviço",
                    doc.get("count", Number.class).longValue()
            });
        }
        return rows;
    }

    @Override
    public long countAtrasosInformadosBetween(LocalDateTime desde, LocalDateTime ate) {
        Query query = new Query(Criteria.where("inicio").gte(desde).lt(ate)
                .and("atrasoInformadoEm").ne(null));
        return mongoTemplate.count(query, Agendamento.class);
    }

    @Override
    public List<Object[]> countGroupedByBarbeiroAndStatusBetween(LocalDateTime desde, LocalDateTime ate) {
        Aggregation agg = newAggregation(
                match(Criteria.where("inicio").gte(desde).lt(ate)),
                group("barbeiroId", "status").count().as("count"),
                LookupOperation.newLookup().from("barbeiros").localField("_id.barbeiroId").foreignField("_id").as("barbeiro"),
                unwind("barbeiro", true),
                project("count")
                        .and("_id.barbeiroId").as("barbeiroId")
                        .and("barbeiro.nome").as("nome")
                        .and("_id.status").as("status"),
                sort(Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("status"))));
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "agendamentos", Document.class);
        List<Object[]> rows = new ArrayList<>();
        for (Document doc : results.getMappedResults()) {
            StatusAgendamento status = parseStatus(doc.get("status"));
            UUID barbeiroId = doc.get("barbeiroId", UUID.class);
            if (status == null || barbeiroId == null) {
                continue;
            }
            rows.add(new Object[]{
                    barbeiroId,
                    doc.getString("nome") != null ? doc.getString("nome") : "Barbeiro",
                    status,
                    doc.get("count", Number.class).longValue()
            });
        }
        return rows;
    }

    @Override
    public List<Agendamento> findSubsequentesBarbeiro(
            UUID barbeiroId,
            LocalDateTime inicioMin,
            List<StatusAgendamento> statuses,
            UUID excluirId) {
        Query query = new Query(Criteria.where("barbeiroId").is(barbeiroId)
                .and("inicio").gt(inicioMin)
                .and("status").in(statuses)
                .and("_id").ne(excluirId));
        query.with(Sort.by(Sort.Direction.ASC, "inicio"));
        return mongoTemplate.find(query, Agendamento.class);
    }

    @Override
    public List<Agendamento> findConcluidosNoPeriodo(LocalDateTime desde, LocalDateTime ate) {
        Query query = new Query(Criteria.where("inicio").gte(desde).lt(ate)
                .and("status").is(StatusAgendamento.CONCLUIDO));
        return mongoTemplate.find(query, Agendamento.class);
    }

    @Override
    public List<Agendamento> findAgendamentosNoPeriodo(LocalDateTime desde, LocalDateTime ate) {
        Query query = new Query(Criteria.where("inicio").gte(desde).lt(ate));
        return mongoTemplate.find(query, Agendamento.class);
    }

    @Override
    public List<Agendamento> findOcupadosBarbeiroNoPeriodo(
            UUID barbeiroId,
            LocalDateTime periodoInicio,
            LocalDateTime periodoFim,
            List<StatusAgendamento> statuses) {
        Query query = new Query(Criteria.where("barbeiroId").is(barbeiroId)
                .and("status").in(statuses)
                .and("inicio").lt(periodoFim)
                .and("fim").gt(periodoInicio));
        query.with(Sort.by(Sort.Direction.ASC, "inicio"));
        return mongoTemplate.find(query, Agendamento.class);
    }

    private List<Object[]> mapBarbeiroCountRows(Aggregation agg) {
        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "agendamentos", Document.class);
        List<Object[]> rows = new ArrayList<>();
        for (Document doc : results.getMappedResults()) {
            UUID barbeiroId = doc.get("barbeiroId", UUID.class);
            if (barbeiroId == null) {
                continue;
            }
            rows.add(new Object[]{
                    barbeiroId,
                    doc.getString("nome") != null ? doc.getString("nome") : "Barbeiro",
                    doc.get("count", Number.class).longValue()
            });
        }
        return rows;
    }

    private static StatusAgendamento parseStatus(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof StatusAgendamento status) {
            return status;
        }
        try {
            return StatusAgendamento.valueOf(raw.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
