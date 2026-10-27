package com.capysoft.identity.security.infrastructure.external;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.capysoft.identity.security.application.dto.AccessTokenClaims;
import com.capysoft.identity.security.application.port.out.TokenRevocationPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Lista de revocación en Redis, compartida por todas las réplicas y servicios:
 * <ul>
 *   <li>{@code tuevento:revocation:user:<userId>} = epoch ms del corte: los tokens del usuario
 *       emitidos antes no valen. Vive lo que dura un access token (más un margen), porque pasado
 *       ese tiempo ya no queda ninguno emitido antes del corte.
 *   <li>{@code tuevento:revocation:jti:<jti>} = un token concreto, hasta que expira.
 * </ul>
 * Una sola ida a Redis por petición (MGET de ambas claves). Fail-closed: cualquier error de Redis
 * se convierte en {@link TokenRevocationUnavailableException}.
 */
@Slf4j
public class RedisTokenRevocationStore implements TokenRevocationPort {

    static final String USER_KEY_PREFIX = "tuevento:revocation:user:";
    static final String TOKEN_KEY_PREFIX = "tuevento:revocation:jti:";
    /** Margen por desfase de reloj entre réplicas. */
    private static final Duration SKEW = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;
    private final Duration userCutoffTtl;
    private final Clock clock;

    public RedisTokenRevocationStore(StringRedisTemplate redis, Duration accessTokenLifetime, Clock clock) {
        this.redis = redis;
        this.userCutoffTtl = accessTokenLifetime.plus(SKEW);
        this.clock = clock;
    }

    @Override
    public void revokeUser(Integer userId, Instant revokedAt) {
        try {
            String key = USER_KEY_PREFIX + userId;
            String previous = redis.opsForValue().get(key);
            // Nunca adelantar el corte hacia atrás: si ya había uno posterior, se conserva
            long cutoff = previous == null ? revokedAt.toEpochMilli()
                    : Math.max(Long.parseLong(previous), revokedAt.toEpochMilli());
            redis.opsForValue().set(key, String.valueOf(cutoff), userCutoffTtl);
        } catch (RuntimeException e) {
            throw new TokenRevocationUnavailableException("Could not revoke tokens of user " + userId, e);
        }
    }

    @Override
    public void revokeToken(String tokenId, Instant expiresAt) {
        if (tokenId == null) {
            return;
        }
        Duration ttl = expiresAt == null ? userCutoffTtl
                : Duration.between(clock.instant(), expiresAt).plus(SKEW);
        if (ttl.isNegative() || ttl.isZero()) {
            return; // ya expiró: no hace falta recordarlo
        }
        try {
            redis.opsForValue().set(TOKEN_KEY_PREFIX + tokenId, "1", ttl);
        } catch (RuntimeException e) {
            throw new TokenRevocationUnavailableException("Could not revoke token " + tokenId, e);
        }
    }

    @Override
    public boolean isRevoked(AccessTokenClaims claims) {
        List<String> keys = new ArrayList<>(2);
        keys.add(USER_KEY_PREFIX + claims.userId());
        if (claims.tokenId() != null) {
            keys.add(TOKEN_KEY_PREFIX + claims.tokenId());
        }
        List<String> values;
        try {
            values = redis.opsForValue().multiGet(keys);
        } catch (RuntimeException e) {
            throw new TokenRevocationUnavailableException("Token revocation store unavailable", e);
        }
        if (values == null) {
            throw new TokenRevocationUnavailableException("Token revocation store returned no answer", null);
        }
        if (values.size() > 1 && values.get(1) != null) {
            return true;
        }
        String cutoff = values.isEmpty() ? null : values.get(0);
        if (cutoff == null) {
            return false;
        }
        // Sin fecha de emisión no se puede saber si es anterior al corte: se rechaza
        return claims.issuedAt() == null || claims.issuedAt().toEpochMilli() < Long.parseLong(cutoff);
    }
}
