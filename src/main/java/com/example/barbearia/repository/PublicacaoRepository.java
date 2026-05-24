package com.example.barbearia.repository;

import com.example.barbearia.domain.Publicacao;
import com.example.barbearia.domain.TipoPublicacao;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PublicacaoRepository extends MongoRepository<Publicacao, UUID> {

    List<Publicacao> findByPublicadoTrueOrderByPublicadoEmDesc();

    List<Publicacao> findByPublicadoTrueAndTipoOrderByPublicadoEmDesc(TipoPublicacao tipo);

    List<Publicacao> findAllByOrderByPublicadoEmDesc();

    default List<Publicacao> findAllWithMidias() {
        return findAllByOrderByPublicadoEmDesc();
    }
}
