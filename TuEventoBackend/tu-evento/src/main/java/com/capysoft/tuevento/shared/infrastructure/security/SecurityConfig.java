package com.capysoft.tuevento.shared.infrastructure.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenGeneratorPort tokenGenerator;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenGenerator);
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/storage/**",
            "/api/v1/geolocation/**",
            "/api/v1/profiles/*/",
            "/api/v1/profiles/user/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/actuator/**"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/v1/themes",
            "/api/v1/events/*",
            "/api/v1/events/public",
            "/api/v1/events/public/**",
            "/api/v1/events/status/**",
            "/api/v1/events/*/layout",   // GET layout de un evento — público para cargar el editor
            "/api/v1/events/*/ratings",  // GET ratings de un evento — público
            "/api/v1/events/*/media",    // GET media de un evento — público
            "/api/v1/ratings/*/replies", // GET replies de un rating — público
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/geolocation/sites").hasAnyAuthority("ADMIN", "ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/v1/geolocation/sites/**").hasAnyAuthority("ADMIN", "ORGANIZER")
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
                        // Estos matchers son necesarios porque PUBLIC_GET_ENDPOINTS solo registra GET.
                        // Sin estas reglas explícitas, los verbos restantes caerían en anyRequest()
                        // y @PreAuthorize podría bloquearse antes de llegar al use case (403 de Spring,
                        // no el error de negocio del use case).
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/api/v1/events/**").hasAuthority("ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/v1/events/**").hasAuthority("ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                "/api/v1/events/**").hasAuthority("ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/events/*/media").hasAuthority("ORGANIZER")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/events/*/ratings").hasAuthority("USER")
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
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
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
