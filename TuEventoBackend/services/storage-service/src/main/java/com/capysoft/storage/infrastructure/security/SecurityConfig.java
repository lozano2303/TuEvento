package com.capysoft.storage.infrastructure.security;

import java.util.List;

import com.capysoft.common.infrastructure.security.SecurityUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(SecurityConfig::toAuthentication))
                );
        return http.build();
    }

    /**
     * Convierte el JWT de Identidad en un {@link SecurityUser} con los mismos claims que usa el
     * monolito ({@code userId}, {@code role}, {@code sub} = alias), para que los controladores
     * reciban el usuario con {@code @AuthenticationPrincipal SecurityUser}. La autoridad es el rol
     * tal cual ({@code ADMIN}, {@code ORGANIZER}, {@code USER}), igual que en el monolito.
     */
    static AbstractAuthenticationToken toAuthentication(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        SecurityUser user = new SecurityUser(
                userId != null ? userId.intValue() : null, jwt.getSubject(), jwt.getClaimAsString("role"));
        return new UsernamePasswordAuthenticationToken(user, jwt,
                user.getRole() != null ? user.getAuthorities() : List.of());
    }
}
