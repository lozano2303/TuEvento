package com.capysoft.tuevento.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Provides a {@link Clock} bean fixed to the {@code America/Bogota} timezone.
 *
 * <p>Injecting {@code Clock} instead of calling {@code LocalDate.now()} directly
 * makes every use case that depends on "now" fully testable: tests can supply a
 * {@link Clock#fixed fixed} clock to obtain deterministic, timezone-aware dates.
 */
@Configuration
public class ClockConfig {

    /** Colombia Standard Time — UTC-5, no DST. */
    public static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    @Bean
    public Clock clock() {
        return Clock.system(BOGOTA_ZONE);
    }
}
