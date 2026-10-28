package com.capysoft.tuevento.shared.infrastructure.security.token;

import java.time.Instant;
import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifica los access tokens con el JWK Set que publica identity-service (el monolito ya no firma
 * tokens ni tiene la clave privada). El decoder guarda las claves en caché y las vuelve a pedir
 * si llega un {@code kid} desconocido; si Identidad no responde, el token no se acepta.
 */
@Slf4j
public class JwksAccessTokenVerifier implements AccessTokenVerifier {

    private static final String ISSUED_AT_MS_CLAIM = "iatMs";

    private final JwtDecoder decoder;

    public JwksAccessTokenVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Optional<AccessTokenClaims> verify(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtException e) {
            log.debug("JWT rejected: {}", e.getMessage());
            return Optional.empty();
        }
        Number userId = jwt.getClaim("userId");
        String role = jwt.getClaimAsString("role");
        if (userId == null || role == null || role.isBlank()) {
            log.debug("JWT without userId or role ignored (not an access token)");
            return Optional.empty();
        }
        return Optional.of(new AccessTokenClaims(userId.intValue(), jwt.getSubject() == null ? "" : jwt.getSubject(),
                role, jwt.getId(), issuedAt(jwt), jwt.getExpiresAt()));
    }

    private static Instant issuedAt(Jwt jwt) {
        Object ms = jwt.getClaims().get(ISSUED_AT_MS_CLAIM);
        if (ms instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return jwt.getIssuedAt();
    }
}
