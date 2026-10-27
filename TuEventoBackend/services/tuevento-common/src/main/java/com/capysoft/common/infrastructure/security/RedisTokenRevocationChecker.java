package com.capysoft.common.infrastructure.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Consulta la lista de revocación de access tokens que escribe Identidad en Redis. El formato de
 * las claves es contrato con {@code RedisTokenRevocationStore} del monolito:
 * <ul>
 *   <li>{@code tuevento:revocation:user:<userId>} = epoch ms; los tokens emitidos antes no valen.
 *   <li>{@code tuevento:revocation:jti:<jti>} = el token concreto se cerró con logout.
 * </ul>
 * Una sola ida a Redis (MGET). Fail-closed: si Redis falla lanza
 * {@link TokenRevocationUnavailableException}.
 */
public class RedisTokenRevocationChecker {

    public static final String USER_KEY_PREFIX = "tuevento:revocation:user:";
    public static final String TOKEN_KEY_PREFIX = "tuevento:revocation:jti:";

    private final StringRedisTemplate redis;

    public RedisTokenRevocationChecker(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * @param tokenId  claim {@code jti}; puede ser null
     * @param issuedAt emisión con precisión de ms; null si se desconoce (con un corte activo se rechaza)
     */
    public boolean isRevoked(Integer userId, String tokenId, Instant issuedAt) {
        List<String> keys = new ArrayList<>(2);
        keys.add(USER_KEY_PREFIX + userId);
        if (tokenId != null) {
            keys.add(TOKEN_KEY_PREFIX + tokenId);
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
        return issuedAt == null || issuedAt.toEpochMilli() < Long.parseLong(cutoff);
    }

    /** No se pudo consultar la lista: la petición no debe aceptarse. */
    public static class TokenRevocationUnavailableException extends RuntimeException {
        public TokenRevocationUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
