package com.capysoft.geolocation.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Rellena las columnas de auditoría de idiomas y trabajos de traducción (created_by es NOT NULL),
 * igual que el monolito: el usuario autenticado, o "system" en lo que llega sin usuario (órdenes
 * de Kafka, planificadores). Va en su propia configuración y no en la clase principal para que los
 * tests de capa web (@WebMvcTest, sin JPA) puedan arrancar.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    static final String SYSTEM = "system";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of(SYSTEM);
            }
            return Optional.ofNullable(auth.getName());
        };
    }
}
