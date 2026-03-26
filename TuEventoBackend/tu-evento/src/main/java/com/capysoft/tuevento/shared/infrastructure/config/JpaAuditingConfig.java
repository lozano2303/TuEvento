package com.capysoft.tuevento.shared.infrastructure.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enables Spring Data JPA auditing.
 * Provides the current authenticated user's ID to populate createdBy / updatedBy fields.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Resolves the current auditor (user ID) from the Spring Security context.
     * Returns empty if no authenticated user is present (e.g. system operations).
     */
    @Bean
    public AuditorAware<Integer> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return Optional.empty();
            }
            // Expects the principal to expose the user ID as an Integer.
            // Adapt this once the security module's UserDetails implementation is in place.
            try {
                return Optional.of(Integer.parseInt(auth.getName()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        };
    }
}
