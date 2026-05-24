package com.example.barbearia.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1️⃣ Ignora rotas públicas (H2 console, auth e página estática de reset)
        String path = request.getRequestURI();
        if (path.startsWith("/h2-console")
                || path.startsWith("/auth/")
                || path.startsWith("/chatbot/")
                || "/redefinir-senha.html".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2️⃣ Pega o header Authorization
        final String authHeader = request.getHeader("Authorization");

        // DEBUG HEADER
        System.out.println("🔍 [JWT Filter] Request URI: " + request.getRequestURI());
        System.out.println("🔍 [JWT Filter] Authorization Header: " + (authHeader != null ? "Presente" : "Ausente"));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("⚠️ [JWT Filter] Token não encontrado ou formato inválido. Continuando sem autenticação.");
            filterChain.doFilter(request, response);
            return;
        }

        // 3️⃣ Extrai JWT e usuário
        final String jwt = authHeader.substring(7);
        
        // DEBUG TOKEN
        System.out.println("🔍 [JWT Filter] Token extraído (primeiros 20 chars): " + (jwt.length() > 20 ? jwt.substring(0, 20) + "..." : jwt));

        try {
            // 4️⃣ Extrai email do token
            String userEmail = jwtService.extractUsername(jwt);
            System.out.println("✅ [JWT Filter] Email extraído do token: " + userEmail);

            if (userEmail == null || userEmail.isEmpty()) {
                System.out.println("❌ [JWT Filter] Email extraído é nulo ou vazio");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido: email não encontrado");
                return;
            }

            // 5️⃣ Autentica usuário se token válido
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    System.out.println("🔍 [JWT Filter] Carregando UserDetails para: " + userEmail);
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    System.out.println("✅ [JWT Filter] UserDetails carregado: " + userDetails.getUsername());

                    System.out.println("🔍 [JWT Filter] Validando token...");
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        // Verifica se a autenticação foi salva corretamente
                        var savedAuth = SecurityContextHolder.getContext().getAuthentication();
                        System.out.println("✅ [JWT Filter] Usuário autenticado com sucesso: " + userDetails.getUsername());
                        System.out.println("🔍 [JWT Filter] Verificando SecurityContext após autenticação:");
                        System.out.println("   - Authentication no Context: " + (savedAuth != null ? savedAuth.getName() : "NULL"));
                        System.out.println("   - Authorities: " + (savedAuth != null ? savedAuth.getAuthorities() : "NULL"));
                    } else {
                        System.out.println("❌ [JWT Filter] Token inválido ou expirado após validação");
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou expirado");
                        return;
                    }
                } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                    System.out.println("❌ [JWT Filter] Usuário não encontrado no banco: " + userEmail);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuário não encontrado");
                    return;
                } catch (Exception e) {
                    System.out.println("❌ [JWT Filter] Erro ao carregar usuário: " + e.getMessage());
                    e.printStackTrace();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erro ao processar autenticação");
                    return;
                }
            } else {
                System.out.println("ℹ️ [JWT Filter] Usuário já autenticado: " + SecurityContextHolder.getContext().getAuthentication().getName());
            }
        } catch (ExpiredJwtException e) {
            System.out.println("❌ [JWT Filter] Token expirado: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expirado");
            return;
        } catch (JwtException e) {
            System.out.println("❌ [JWT Filter] Erro ao processar token JWT: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido: " + e.getMessage());
            return;
        } catch (Exception e) {
            System.out.println("❌ [JWT Filter] Erro inesperado ao processar token: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erro ao processar token");
            return;
        }

        // Verifica o SecurityContext antes de passar para o próximo filtro
        var authBeforeChain = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("🔍 [JWT Filter] SecurityContext ANTES do filterChain: " + (authBeforeChain != null ? authBeforeChain.getName() : "NULL"));
        
        filterChain.doFilter(request, response);
        
        // Verifica o SecurityContext depois do filterChain
        var authAfterChain = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("🔍 [JWT Filter] SecurityContext DEPOIS do filterChain: " + (authAfterChain != null ? authAfterChain.getName() : "NULL"));
    }
}