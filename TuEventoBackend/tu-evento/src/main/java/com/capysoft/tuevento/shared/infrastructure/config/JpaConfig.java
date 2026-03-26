package com.capysoft.tuevento.shared.infrastructure.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Configuración de JPA. Habilita la auditoría automática de entidades
 * mediante Spring Data JPA ({@code @EnableJpaAuditing}).
 *
 * Provee el auditor actual desde el contexto de seguridad de Spring.
 * Retorna vacío para operaciones del sistema sin usuario autenticado.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Resuelve el identificador del usuario autenticado actual.
     * Se adapta al módulo de seguridad una vez que UserDetails esté implementado.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.empty();
            }
            return Optional.ofNullable(auth.getName());
        };
    }
}
