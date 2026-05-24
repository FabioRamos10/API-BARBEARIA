package com.example.barbearia.mail;

import java.util.Locale;

public enum PixTipoChave {
    CPF,
    TELEFONE,
    EMAIL,
    ALEATORIA;

    public static PixTipoChave fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return CPF;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "TELEFONE", "PHONE", "CELULAR" -> TELEFONE;
            case "EMAIL", "E-MAIL" -> EMAIL;
            case "ALEATORIA", "EVP", "RANDOM" -> ALEATORIA;
            default -> CPF;
        };
    }
}
