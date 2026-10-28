package com.capysoft.identity.shared.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Rellena created_at/updated_at de las entidades. Va en su propia configuración y no en la
 * clase principal para que los tests de capa web (@WebMvcTest, sin JPA) puedan arrancar.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
