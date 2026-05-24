package com.example.barbearia.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String DEV_FALLBACK_SECRET = "chave_super_secreta_barbearia_2026_jwt_dev_only";

    private final String secretKey;
    private static final long EXPIRATION = 1000 * 60 * 60 * 2; // 2 horas

    public JwtService(@Value("${app.jwt.secret:}") String secretKey) {
        this.secretKey = (secretKey == null || secretKey.isBlank()) ? DEV_FALLBACK_SECRET : secretKey;
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /* ===================== TOKEN ===================== */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /* ===================== EXTRAÇÕES ===================== */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            System.out.println("❌ [JWT Service] Token expirado");
            throw e;
        } catch (MalformedJwtException e) {
            System.out.println("❌ [JWT Service] Token malformado: " + e.getMessage());
            throw e;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            System.out.println("❌ [JWT Service] Assinatura do token inválida: " + e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            System.out.println("❌ [JWT Service] Token vazio ou nulo");
            throw e;
        } catch (JwtException e) {
            System.out.println("❌ [JWT Service] Erro ao processar token: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw e;
        }
    }

    /* ===================== VALIDAÇÃO ===================== */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean usernameMatches = username != null && username.equals(userDetails.getUsername());
            boolean notExpired = !isTokenExpired(token);
            boolean valid = usernameMatches && notExpired;
            
            System.out.println("🔍 [JWT Service] Validando token...");
            System.out.println("   - Username do token: " + username);
            System.out.println("   - Username do UserDetails: " + userDetails.getUsername());
            System.out.println("   - Usernames correspondem: " + usernameMatches);
            System.out.println("   - Token não expirado: " + notExpired);
            System.out.println("   - Token válido: " + valid);
            
            return valid;
        } catch (Exception e) {
            System.out.println("❌ [JWT Service] Erro ao validar token: " + e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            boolean expired = expiration.before(new Date());
            System.out.println("🔍 [JWT Service] Data de expiração: " + expiration);
            System.out.println("🔍 [JWT Service] Data atual: " + new Date());
            System.out.println("🔍 [JWT Service] Token expirado: " + expired);
            return expired;
        } catch (Exception e) {
            System.out.println("❌ [JWT Service] Erro ao verificar expiração: " + e.getMessage());
            return true; // Se não conseguir verificar, considera expirado por segurança
        }
    }
}
