package com.capysoft.tuevento.modules.ticket.infrastructure.external;

import com.capysoft.tuevento.modules.ticket.domain.port.QrNonceStore;
import com.capysoft.tuevento.modules.ticket.domain.port.QrTokenPort.QrScope;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Almacén de nonces de QR respaldado por Redis.
 *
 * Clave: {@code ticket:qr:TICKET:{id}} / {@code ticket:qr:ORDER:{id}}.
 * - store: SET con expiración (pisa el valor anterior → invalida el QR previo).
 * - consume: GETDEL (lee y borra atómicamente) y compara con el nonce entrante.
 *
 * Fail-closed: si Redis no está disponible, consume() devuelve false (el escaneo
 * se rechaza) en lugar de permitir el acceso.
 */
@Component
public class RedisQrNonceStore implements QrNonceStore {

    private static final String KEY_PREFIX = "ticket:qr:";

    /**
     * Opcional: en perfiles que excluyen la autoconfiguración de Redis (p. ej.
     * tests) no existe un StringRedisTemplate. En ese caso el store degrada a
     * fail-closed (consume siempre false) sin romper el arranque del contexto.
     */
    private final ObjectProvider<StringRedisTemplate> redisProvider;

    public RedisQrNonceStore(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    private String key(QrScope scope, Long targetId) {
        return KEY_PREFIX + scope.name() + ":" + targetId;
    }

    @Override
    public void store(QrScope scope, Long targetId, String nonce, long ttlSeconds) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        redis.opsForValue().set(key(scope, targetId), nonce, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean consume(QrScope scope, Long targetId, String nonce) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            // Sin Redis no hay anti-reventa posible → rechazar (fail-closed).
            return false;
        }
        try {
            // GETDEL: devuelve el valor actual y lo borra en una sola operación atómica.
            String stored = redis.opsForValue().getAndDelete(key(scope, targetId));
            return stored != null && stored.equals(nonce);
        } catch (RuntimeException e) {
            // Redis caído o error de conexión → rechazar (fail-closed).
            return false;
        }
    }

    @Override
    public void invalidate(QrScope scope, Long targetId) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.delete(key(scope, targetId));
        } catch (RuntimeException e) {
            // best-effort: el exp del JWT igual caduca el QR
        }
    }
}
