package com.capysoft.identity.security.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface TokenGeneratorPort {

    String generateAccessToken(Integer userId, String alias, String role);
    String generateRefreshToken(Integer userId);
    Optional<Integer> extractUserId(String token);
    Optional<String> extractRole(String token);
    Optional<String> extractSubject(String token);
    boolean isTokenValid(String token);

    /**
     * {@code jti}, momento de emisión y expiración de un token válido, para la lista de revocación.
     * Vacío si el token no es válido.
     */
    Optional<TokenMetadata> extractMetadata(String token);

    /**
     * @param tokenId  claim {@code jti}; {@code null} si el token no lo trae
     * @param issuedAt emisión con precisión de milisegundos (claim {@code iatMs}; en tokens
     *                 anteriores, {@code iat} en segundos)
     */
    record TokenMetadata(String tokenId, Instant issuedAt, Instant expiresAt) {
    }
}
