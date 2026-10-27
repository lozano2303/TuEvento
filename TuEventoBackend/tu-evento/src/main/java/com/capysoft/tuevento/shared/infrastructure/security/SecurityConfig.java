package com.capysoft.tuevento.shared.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenRevocationPort;
import com.capysoft.tuevento.modules.security.infrastructure.external.InMemoryTokenRevocationStore;
import com.capysoft.tuevento.modules.security.infrastructure.external.RedisTokenRevocationStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenGeneratorPort tokenGenerator;
    private final ObjectMapper       objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(TokenRevocationPort tokenRevocationPort) {
        return new JwtAuthenticationFilter(tokenGenerator, tokenRevocationPort, objectMapper);
    }

    /**
     * Lista de revocación de access tokens. Con Redis configurado (dev, k8s, producción) es Redis
     * y, si Redis no responde, las peticiones con token se rechazan (fail-closed). Solo sin Redis
     * configurado, que únicamente ocurre en los tests, se usa una lista en memoria.
     */
    @Bean
    public TokenRevocationPort tokenRevocationPort(ObjectProvider<StringRedisTemplate> redis,
            ObjectProvider<Clock> clockProvider,
            @Value("${app.jwt.access-token-expiration:900000}") long accessTokenExpirationMs) {
        Clock clock = clockProvider.getIfAvailable(Clock::systemUTC);
        StringRedisTemplate template = redis.getIfAvailable();
        if (template == null) {
            log.warn("Redis is not configured: JWT revocation list kept in memory (tests only, not shared across replicas)");
            return new InMemoryTokenRevocationStore(clock);
        }
        return new RedisTokenRevocationStore(template, Duration.ofMillis(accessTokenExpirationMs), clock);
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/profiles/*/",
            "/api/v1/profiles/user/**",
            "/api/v1/webhooks/**",  // Webhook endpoints — autenticación por HMAC signature
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/ws/**",              // WebSocket endpoint con SockJS (web + móvil) — autenticación en JwtHandshakeInterceptor
            "/.well-known/jwks.json" // JWK Set público — clave pública RSA para verificar JWT
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/v1/themes",
            "/api/v1/languages",          // GET idiomas activos — público
            "/api/v1/languages/*",        // GET idioma por ID — público
            "/api/v1/events/*",
            "/api/v1/events/public",
            "/api/v1/events/public/**",
            "/api/v1/events/status/**",
            "/api/v1/events/*/layout",   // GET layout de un evento — público para cargar el editor
            "/api/v1/events/*/ratings",  // GET ratings de un evento — público
            "/api/v1/events/*/media",    // GET media de un evento — público
            "/api/v1/categories/**",
            "/api/v1/category-events/**",
            "/api/v1/section-types",
            "/api/v1/event-sections/event/**",
            "/api/v1/seat-blocks/section/**",
            "/api/v1/seats/block/**",
            "/api/v1/seats/section/**",
            "/api/v1/seats/*/log"
    };

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    /**
     * API interna entre servicios ({@code /internal/**}): solo con la clave de
     * {@link InternalApiKeyFilter}, nunca con el JWT de un usuario. El gateway no la enruta, pero el
     * ingress de k8s sí llega al backend, así que la clave es la única barrera.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public SecurityFilterChain internalFilterChain(HttpSecurity http,
            @Value("${app.internal.api-key:}") String internalApiKey) throws Exception {
        return http
                .securityMatcher("/internal/**")
                // CSRF is safe to disable: this chain is stateless and authenticates only with the
                // X-Internal-Api-Key header, never with cookies, so a browser cannot forge the request.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 header-only service auth
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        // 401 — sin autenticación válida (token ausente, inválido o expirado)
                        // El interceptor del frontend escucha este código para disparar el refresh.
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Authentication required\",\"data\":null}");
                        })
                        // 403 — autenticado pero sin el rol/autoridad requerida.
                        // El frontend NO debe disparar refresh en este caso — es un problema de permisos,
                        // no de sesión expirada.
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Access denied\",\"data\":null}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/categories/**").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/v1/categories/**").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/categories/**").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/category-events/**").hasAnyAuthority("ADMIN", "ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/category-events/**").hasAnyAuthority("ADMIN", "ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/themes/activate/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/v1/themes/my-active").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/v1/themes/my-active/customize").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/themes/my-active/customize/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/v1/themes/my-active/log").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/v1/events/*/layout").authenticated()
                        // ── Verbos no-GET en rutas cubiertas por PUBLIC_GET_ENDPOINTS ──────────────
                        // ── Verbos no-GET en rutas cubiertas por PUBLIC_GET_ENDPOINTS ──────────────
                        // Estos matchers son necesarios porque PUBLIC_GET_ENDPOINTS solo registra GET.
                        // Sin estas reglas explícitas, los verbos restantes caerían en anyRequest()
                        // y @PreAuthorize podría bloquearse antes de llegar al use case (403 de Spring,
                        // no el error de negocio del use case).
                        // ── Ratings: cualquier usuario autenticado puede comentar/borrar/editar (R1) ──
                        // DELETE y PATCH /ratings/* van ANTES de DELETE/PATCH /events/** (ORGANIZER).
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/events/*/ratings/*").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                "/api/v1/events/*/ratings/*").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/events/*/ratings").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/events/**").hasAuthority("ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/v1/events/**").hasAuthority("ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                "/api/v1/events/**").hasAuthority("ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/events/*/media").hasAuthority("ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/event-sections").hasAnyAuthority("ORGANIZER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/v1/event-sections/**").hasAnyAuthority("ORGANIZER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/event-sections/**").hasAnyAuthority("ORGANIZER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/seats").hasAnyAuthority("ORGANIZER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                "/api/v1/seats/**").hasAnyAuthority("ORGANIZER", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/seats/**").hasAnyAuthority("ORGANIZER", "ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                        // ── Language admin endpoints ──
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/admin/languages").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/v1/admin/languages/**").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                "/api/v1/admin/languages/**").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/admin/languages/*/set-default").hasAuthority("ADMIN")
                        // ── Wallet — los GET caen en anyRequest().authenticated()
                        // Los POST tienen @PreAuthorize en el controller, pero necesitan
                        // pasar el filtro de SecurityConfig como authenticated() primero.
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/wallet/credit").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/wallet/adjust").hasAuthority("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Permite al frontend leer cuánto esperar tras un 429 del rate limit
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
