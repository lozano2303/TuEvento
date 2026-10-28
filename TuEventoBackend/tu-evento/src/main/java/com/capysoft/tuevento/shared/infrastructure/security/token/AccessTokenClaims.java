package com.capysoft.tuevento.shared.infrastructure.security.token;

import java.time.Instant;

/**
 * Datos de un access token ya validado (firma y expiración).
 *
 * @param tokenId   claim {@code jti}; {@code null} en tokens emitidos antes de que existiera
 * @param issuedAt  momento de emisión con precisión de milisegundos (claim {@code iatMs}, o
 *                  {@code iat} en segundos en los tokens anteriores)
 * @param expiresAt claim {@code exp}
 */
public record AccessTokenClaims(
        Integer userId,
        String alias,
        String role,
        String tokenId,
        Instant issuedAt,
        Instant expiresAt) {
}
