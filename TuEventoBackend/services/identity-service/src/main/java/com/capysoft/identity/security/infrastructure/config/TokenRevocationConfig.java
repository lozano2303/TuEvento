package com.capysoft.identity.security.infrastructure.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.capysoft.identity.security.application.port.out.TokenRevocationPort;
import com.capysoft.identity.security.infrastructure.external.InMemoryTokenRevocationStore;
import com.capysoft.identity.security.infrastructure.external.RedisTokenRevocationStore;

import lombok.extern.slf4j.Slf4j;

/**
 * Lista de revocación que escribe Identidad al cerrar sesión, desactivar la cuenta o quitar un rol:
 * la misma instancia de Redis y las mismas claves que el monolito, así lo que revoca uno lo
 * respetan todos. Solo sin Redis (únicamente en tests) se usa una lista en memoria.
 */
@Slf4j
@Configuration
public class TokenRevocationConfig {

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
}
