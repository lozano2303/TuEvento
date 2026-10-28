package com.capysoft.tuevento.shared.infrastructure.security.token;

import java.util.Optional;

/**
 * Valida un access token emitido por Identidad (firma RS256 y expiración) y devuelve sus claims.
 * Vacío si el token no es válido, expiró o no es un access token (el refresh token se firma igual
 * pero no lleva rol). La revocación se comprueba aparte, con {@link TokenRevocationPort}.
 */
public interface AccessTokenVerifier {

    Optional<AccessTokenClaims> verify(String token);
}
