package com.capysoft.geolocation.infrastructure.security;

import com.capysoft.common.infrastructure.security.SecurityUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
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
                        // Departamentos, ciudades y sedes se leen sin sesión (como en el monolito)
                        .requestMatchers(HttpMethod.GET, "/api/v1/geolocation/**").permitAll()
                        // Crear y editar sedes: ADMIN u ORGANIZER (también con @PreAuthorize en el controlador)
                        .requestMatchers(HttpMethod.POST, "/api/v1/geolocation/sites").hasAnyAuthority("ADMIN", "ORGANIZER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/geolocation/sites/**").hasAnyAuthority("ADMIN", "ORGANIZER")
                        // Idiomas activos e idioma por id: públicos; administrarlos, solo ADMIN (como en el monolito)
                        .requestMatchers(HttpMethod.GET, "/api/v1/languages", "/api/v1/languages/*").permitAll()
                        .requestMatchers("/api/v1/admin/languages", "/api/v1/admin/languages/**").hasAuthority("ADMIN")
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
    public static AbstractAuthenticationToken toAuthentication(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        String role = jwt.getClaimAsString("role");
        // El refresh token se firma con la misma clave pero no lleva rol: solo el access token
        // (15 min) sirve para llamar a la API; el refresh (7 días) solo para /auth/refresh
        if (userId == null || role == null || role.isBlank()) {
            throw new BadCredentialsException("Not an access token");
        }
        SecurityUser user = new SecurityUser(userId.intValue(), jwt.getSubject(), role);
        return new UsernamePasswordAuthenticationToken(user, jwt, user.getAuthorities());
    }
}
