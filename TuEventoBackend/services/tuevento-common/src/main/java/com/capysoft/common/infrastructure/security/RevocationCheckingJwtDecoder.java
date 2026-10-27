package com.capysoft.common.infrastructure.security;

import java.time.Instant;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Decodifica el JWT con el decoder de siempre (firma RS256 contra el JWKS de Identidad y
 * expiración) y después comprueba que Identidad no lo haya revocado.
 * <ul>
 *   <li>Revocado (rol quitado, logout, cuenta desactivada): {@link BadJwtException} → 401; el
 *       cliente renueva y recibe un token con sus permisos actuales.
 *   <li>Lista de revocación caída: {@link RevocationUnavailableException} → 503 con
 *       {@link RevocationAwareAuthenticationEntryPoint} (fail-closed).
 * </ul>
 */
public class RevocationCheckingJwtDecoder implements JwtDecoder {

    private static final String ISSUED_AT_MS_CLAIM = "iatMs";

    private final JwtDecoder delegate;
    private final RedisTokenRevocationChecker checker;

    public RevocationCheckingJwtDecoder(JwtDecoder delegate, RedisTokenRevocationChecker checker) {
        this.delegate = delegate;
        this.checker = checker;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = delegate.decode(token);
        Number userId = jwt.getClaim("userId");
        if (userId == null) {
            return jwt; // el conversor de cada servicio lo rechaza como "Not an access token"
        }
        boolean revoked;
        try {
            revoked = checker.isRevoked(userId.intValue(), jwt.getId(), issuedAt(jwt));
        } catch (RedisTokenRevocationChecker.TokenRevocationUnavailableException e) {
            throw new RevocationUnavailableException(e);
        }
        if (revoked) {
            throw new BadJwtException("Token has been revoked");
        }
        return jwt;
    }

    private static Instant issuedAt(Jwt jwt) {
        Object ms = jwt.getClaims().get(ISSUED_AT_MS_CLAIM);
        if (ms instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return jwt.getIssuedAt();
    }

    /** La lista de revocación no respondió; la respuesta debe ser 503, no 401. */
    public static class RevocationUnavailableException extends JwtException {
        public RevocationUnavailableException(Throwable cause) {
            super("Token revocation store unavailable", cause);
        }
    }
}
