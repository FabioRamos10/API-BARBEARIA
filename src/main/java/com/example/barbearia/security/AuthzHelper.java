package com.example.barbearia.security;

import com.example.barbearia.domain.Agendamento;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

/**
 * Utilitários de autorização por papel (evita duplicar lógica entre services).
 */
public final class AuthzHelper {

    private AuthzHelper() {
    }

    public static boolean isStaff(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_RECEPCIONISTA");
    }

    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    public static String email(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return authentication.getName();
    }

    /** ID do cliente sem depender de @Transient carregado (Mongo guarda só clienteId). */
    public static UUID clienteIdOf(Agendamento agendamento) {
        if (agendamento == null) {
            return null;
        }
        if (agendamento.getClienteId() != null) {
            return agendamento.getClienteId();
        }
        return agendamento.getCliente() != null ? agendamento.getCliente().getId() : null;
    }

    /** ID do barbeiro sem depender de @Transient carregado. */
    public static UUID barbeiroIdOf(Agendamento agendamento) {
        if (agendamento == null) {
            return null;
        }
        if (agendamento.getBarbeiroId() != null) {
            return agendamento.getBarbeiroId();
        }
        return agendamento.getBarbeiro() != null ? agendamento.getBarbeiro().getId() : null;
    }
}
