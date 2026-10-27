package com.capysoft.notification.infrastructure.security;

import com.capysoft.common.infrastructure.security.RedisTokenRevocationChecker;
import com.capysoft.common.infrastructure.security.RevocationAwareAuthenticationEntryPoint;
import com.capysoft.common.infrastructure.security.RevocationCheckingJwtDecoder;
import com.capysoft.common.infrastructure.security.SecurityUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // 503 (no 401) si la lista de revocación no responde: fail-closed sin cerrar la sesión del cliente
                        .authenticationEntryPoint(new RevocationAwareAuthenticationEntryPoint())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(SecurityConfig::toAuthentication))
                );
        return http.build();
    }

    /**
     * Valida la firma con el JWKS de Identidad y después la lista de revocación de Redis: un token
     * con el rol revocado, cerrado con logout o de una cuenta desactivada deja de valer aquí en el
     * acto, igual que en el monolito.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
                                 StringRedisTemplate redis) {
        return new RevocationCheckingJwtDecoder(NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build(),
                new RedisTokenRevocationChecker(redis));
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
