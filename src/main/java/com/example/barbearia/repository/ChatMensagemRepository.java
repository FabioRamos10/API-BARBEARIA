package com.example.barbearia.repository;

import com.example.barbearia.domain.ChatMensagem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMensagemRepository extends MongoRepository<ChatMensagem, UUID> {

    List<ChatMensagem> findByConversaIdOrderByEnviadaEmAsc(UUID conversaId);

    default List<ChatMensagem> findByConversa_IdOrderByEnviadaEmAsc(UUID conversaId) {
        return findByConversaIdOrderByEnviadaEmAsc(conversaId);
    }

    long countByConversaIdAndRemetenteUserIdNotAndLidaFalse(UUID conversaId, UUID remetenteUserId);

    default long countByConversa_IdAndRemetenteUserIdNotAndLidaFalse(UUID conversaId, UUID remetenteUserId) {
        return countByConversaIdAndRemetenteUserIdNotAndLidaFalse(conversaId, remetenteUserId);
    }
}
