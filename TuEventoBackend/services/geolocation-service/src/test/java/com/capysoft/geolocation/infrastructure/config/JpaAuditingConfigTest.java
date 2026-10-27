package com.capysoft.geolocation.infrastructure.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/** Quién figura como autor de los cambios en idiomas y trabajos de traducción. */
@DisplayName("JpaAuditingConfig — auditor")
class JpaAuditingConfigTest {

    private final JpaAuditingConfig config = new JpaAuditingConfig();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sin usuario (Kafka, planificadores) o anónimo: system")
    void system() {
        assertThat(config.auditorProvider().getCurrentAuditor()).contains("system");

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(config.auditorProvider().getCurrentAuditor()).contains("system");
    }

    @Test
    @DisplayName("con usuario autenticado: su nombre")
    void authenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "admin", null, AuthorityUtils.createAuthorityList("ADMIN")));

        assertThat(config.auditorProvider().getCurrentAuditor()).contains("admin");
    }
}
