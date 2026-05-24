package com.example.barbearia.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "conversas")
@CompoundIndex(name = "par_usuarios", def = "{'usuarioMenorId': 1, 'usuarioMaiorId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversa {

    @Id
    private UUID id;

    private UUID usuarioMenorId;

    private UUID usuarioMaiorId;

    private LocalDateTime ultimaMensagemEm;

    private LocalDateTime createdAt;

    public static UUID[] ordenarPar(UUID a, UUID b) {
        if (a.compareTo(b) <= 0) {
            return new UUID[]{a, b};
        }
        return new UUID[]{b, a};
    }
}
