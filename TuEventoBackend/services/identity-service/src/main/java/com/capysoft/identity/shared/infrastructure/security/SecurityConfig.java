package com.capysoft.identity.shared.infrastructure.security;

import com.capysoft.common.infrastructure.security.RedisTokenRevocationChecker;
import com.capysoft.common.infrastructure.security.RevocationAwareAuthenticationEntryPoint;
import com.capysoft.common.infrastructure.security.RevocationCheckingJwtDecoder;
import com.capysoft.common.infrastructure.security.SecurityUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * API interna ({@code /internal/**}) para el monolito y los demás servicios: solo con la clave
     * entre servicios (sin ella, o sin clave configurada, todo se rechaza). El gateway no la enruta.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(HttpSecurity http,
            @Value("${app.internal.api-key:}") String internalApiKey) throws Exception {
        return http
                .securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new InternalApiKeyFilter(internalApiKey), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"code\":\"INTERNAL_AUTH_REQUIRED\","
                                    + "\"message\":\"Internal API key required\"}");
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"code\":\"FORBIDDEN\","
                                    + "\"message\":\"Internal API only\"}");
                        }))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority(InternalApiKeyFilter.AUTHORITY))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // El catálogo de temas se lee sin sesión (como en el monolito); el tema activo,
                        // su personalización y su historial son del usuario autenticado
                        // Login, registro, activación, renovación, recuperación y OAuth: sin sesión (como
                        // en el monolito; el límite de intentos lo aplica AuthRateLimitFilter)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Clave pública RSA para verificar los JWT que firma Identidad
                        .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/themes").permitAll()
                        // El perfil público de un usuario (nombre, foto, biografía) se lee sin sesión;
                        // crearlo y editarlo exige ser su dueño o ADMIN (en ProfileController)
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/user/**").permitAll()
                        // Panel de admin y traducciones de perfiles: solo ADMIN (además del @PreAuthorize)
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        // 503 (no 401) si la lista de revocación no responde: fail-closed sin cerrar la sesión del cliente
                        .authenticationEntryPoint(new RevocationAwareAuthenticationEntryPoint())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(SecurityConfig::toAuthentication))
                );
        return http.build();
    }

    /**
     * En /api/v1/auth/** no se lee la cabecera Authorization: un access token vencido o revocado que
     * el cliente aún adjunte no debe impedir iniciar sesión ni renovar (el monolito hacía lo mismo:
     * ignoraba el token inválido y seguía sin sesión). La excepción es el logout, que necesita saber
     * quién llama para comprobar que la sesión que cierra es suya.
     */
    static BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        return request -> {
            String path = request.getRequestURI();
            boolean authFlow = path.startsWith("/api/v1/auth/") && !path.equals(LOGOUT_PATH);
            return authFlow ? null : resolver.resolve(request);
        };
    }

    static final String LOGOUT_PATH = "/api/v1/auth/logout";

    /**
     * Valida la firma con el JWKS que publica el monolito y después la lista de revocación de
     * Redis: un token cerrado con logout, de una cuenta desactivada o con el rol revocado deja de
     * valer aquí en el acto, igual que en el monolito.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
                                 StringRedisTemplate redis) {
        return new RevocationCheckingJwtDecoder(NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build(),
                new RedisTokenRevocationChecker(redis));
    }

    /**
     * Convierte el JWT en un {@link SecurityUser} con los mismos claims que usa el monolito
     * ({@code userId}, {@code role}, {@code sub} = alias). Solo el access token lleva rol: el
     * refresh token (firmado con la misma clave) no sirve para llamar a la API.
     */
    public static AbstractAuthenticationToken toAuthentication(Jwt jwt) {
        Number userId = jwt.getClaim("userId");
        String role = jwt.getClaimAsString("role");
        if (userId == null || role == null || role.isBlank()) {
            throw new BadCredentialsException("Not an access token");
        }
        SecurityUser user = new SecurityUser(userId.intValue(), jwt.getSubject(), role);
        return new UsernamePasswordAuthenticationToken(user, jwt, user.getAuthorities());
    }
}
