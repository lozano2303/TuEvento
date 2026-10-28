package com.capysoft.tuevento.shared.infrastructure.security.token;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenClaims;
import com.capysoft.tuevento.shared.infrastructure.security.token.TokenRevocationPort;

/**
 * Lista de revocación en memoria, para cuando la aplicación arranca sin Redis configurado (los
 * tests, que excluyen su autoconfiguración). No se comparte entre réplicas: en dev, k8s y
 * producción Redis siempre está configurado y se usa {@link RedisTokenRevocationStore}.
 */
public class InMemoryTokenRevocationStore implements TokenRevocationPort {

    private final Map<Integer, Instant> userCutoffs = new ConcurrentHashMap<>();
    private final Map<String, Instant> revokedTokens = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryTokenRevocationStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void revokeUser(Integer userId, Instant revokedAt) {
        userCutoffs.merge(userId, revokedAt, (a, b) -> a.isAfter(b) ? a : b);
    }

    @Override
    public void revokeToken(String tokenId, Instant expiresAt) {
        if (tokenId != null) {
            revokedTokens.put(tokenId, expiresAt == null ? Instant.MAX : expiresAt);
        }
    }

    @Override
    public boolean isRevoked(AccessTokenClaims claims) {
        if (claims.tokenId() != null) {
            Instant expiresAt = revokedTokens.get(claims.tokenId());
            if (expiresAt != null) {
                if (expiresAt.isAfter(clock.instant())) {
                    return true;
                }
                revokedTokens.remove(claims.tokenId());
            }
        }
        Instant cutoff = userCutoffs.get(claims.userId());
        return cutoff != null && (claims.issuedAt() == null || claims.issuedAt().isBefore(cutoff));
    }
}
