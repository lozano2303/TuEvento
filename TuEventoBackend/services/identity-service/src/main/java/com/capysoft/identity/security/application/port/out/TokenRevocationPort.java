package com.capysoft.identity.security.application.port.out;

import java.time.Instant;

import com.capysoft.identity.security.application.dto.AccessTokenClaims;

/**
 * Lista de revocación de access tokens. Un access token dura 15 min y no se guarda en ningún
 * sitio, así que sin esta lista un rol revocado, un logout o una cuenta desactivada seguirían
 * sirviendo hasta que expire.
 *
 * <p>Fail-closed: si el almacén no responde, todos los métodos lanzan
 * {@link TokenRevocationUnavailableException} y quien llama debe rechazar la petición (o abortar
 * la operación que revoca), nunca darla por buena.
 */
public interface TokenRevocationPort {

    /** Invalida todos los access tokens del usuario emitidos antes de {@code revokedAt}. */
    void revokeUser(Integer userId, Instant revokedAt);

    /** Invalida un token concreto (logout) hasta que expire. */
    void revokeToken(String tokenId, Instant expiresAt);

    /** true si el token está revocado, por sí mismo o por una revocación de su usuario. */
    boolean isRevoked(AccessTokenClaims claims);

    /** El almacén de revocaciones no respondió: no se puede decidir si un token es válido. */
    class TokenRevocationUnavailableException extends RuntimeException {
        public TokenRevocationUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
