package com.capysoft.tuevento.modules.security.application.port.out;

/**
 * Port for verifying a Google ID Token received from the frontend
 * (Google Identity Services credential).
 *
 * Abstracted so it can be mocked in unit tests without hitting Google's servers.
 */
public interface GoogleIdTokenVerifierPort {

    /**
     * Verifies the signature and claims (issuer, audience, expiry) of the given
     * Google ID Token string.
     *
     * @param idToken the raw JWT string received from Google Identity Services
     * @return a {@link GoogleTokenClaims} with the verified payload
     * @throws com.capysoft.tuevento.shared.domain.exception.BusinessException
     *         with code {@code GOOGLE_TOKEN_INVALID} if verification fails for any reason
     */
    GoogleTokenClaims verify(String idToken);

    /** Immutable value object carrying the verified claims we care about. */
    record GoogleTokenClaims(
            String sub,            // Google subject — stable unique user identifier
            String email,
            boolean emailVerified,
            String name            // display name (may be null if scope not granted)
    ) {}
}
