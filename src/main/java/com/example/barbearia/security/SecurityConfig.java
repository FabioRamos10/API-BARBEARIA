package com.example.barbearia.security;

import com.example.barbearia.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        // Configura SecurityContextRepository para modo stateless
        // Isso garante que o SecurityContext seja propagado corretamente entre os filtros
        http.securityContext(context -> context
                .securityContextRepository(new RequestAttributeSecurityContextRepository())
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/redefinir-senha.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/chatbot/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/sobre-nos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/media/publicacoes/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/test").authenticated()
                .anyRequest().authenticated()
        );

        // Bloco de Debug (Pode manter por enquanto)
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    System.out.println("⛔ ERRO 401 (Não Autenticado): " + authException.getMessage());
                    System.out.println("🔍 [SecurityConfig] Authentication no SecurityContext: " + (auth != null ? auth.getName() + " | " + auth.getAuthorities() : "NULL"));
                    System.out.println("🔍 [SecurityConfig] Request URI: " + request.getRequestURI());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erro de autenticação");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    System.out.println("⛔ ERRO 403 (Acesso Negado): " + accessDeniedException.getMessage());
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null) {
                        System.out.println("👤 User: " + auth.getName() + " | Roles: " + auth.getAuthorities());
                    }
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso Negado");
                })
        );

        // Adiciona o filtro JWT injetado como bean
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}