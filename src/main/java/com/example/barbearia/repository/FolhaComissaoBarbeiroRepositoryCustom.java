package com.example.barbearia.repository;

import com.example.barbearia.domain.FolhaComissaoBarbeiro;

import java.util.List;

public interface FolhaComissaoBarbeiroRepositoryCustom {

    List<FolhaComissaoBarbeiro> findByAnoMesOrderByBarbeiroNomeAsc(String anoMes);

    List<FolhaComissaoBarbeiro> findAllByOrderByAnoMesDescBarbeiroNomeAsc();

    List<String> findDistinctAnoMesSortedDesc();
}
