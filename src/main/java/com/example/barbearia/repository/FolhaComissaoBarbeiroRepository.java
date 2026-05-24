package com.example.barbearia.repository;

import com.example.barbearia.domain.FolhaComissaoBarbeiro;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolhaComissaoBarbeiroRepository extends MongoRepository<FolhaComissaoBarbeiro, UUID>, FolhaComissaoBarbeiroRepositoryCustom {

    Optional<FolhaComissaoBarbeiro> findByBarbeiroIdAndAnoMes(UUID barbeiroId, String anoMes);

    default Optional<FolhaComissaoBarbeiro> findByBarbeiro_IdAndAnoMes(UUID barbeiroId, String anoMes) {
        return findByBarbeiroIdAndAnoMes(barbeiroId, anoMes);
    }

    List<FolhaComissaoBarbeiro> findByBarbeiroIdOrderByAnoMesDesc(UUID barbeiroId);

    default List<FolhaComissaoBarbeiro> findByBarbeiro_IdOrderByAnoMesDesc(UUID barbeiroId) {
        return findByBarbeiroIdOrderByAnoMesDesc(barbeiroId);
    }

    List<FolhaComissaoBarbeiro> findByAnoMes(String anoMes);

    List<FolhaComissaoBarbeiro> findByAnoMesIn(Collection<String> anoMeses);

    default List<FolhaComissaoBarbeiro> findByAnoMesOrderByBarbeiro_NomeAsc(String anoMes) {
        return findByAnoMesOrderByBarbeiroNomeAsc(anoMes);
    }

    default List<FolhaComissaoBarbeiro> findAllByOrderByAnoMesDescBarbeiro_NomeAsc() {
        return findAllByOrderByAnoMesDescBarbeiroNomeAsc();
    }

    default List<String> findDistinctAnoMesOrderByAnoMesDesc() {
        return findDistinctAnoMesSortedDesc();
    }
}
