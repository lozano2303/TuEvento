package com.capysoft.tuevento.modules.security.infrastructure.external;

import com.capysoft.tuevento.modules.security.application.port.out.GoogleIdTokenVerifierPort;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * Verifies a Google ID Token (credential) from Google Identity Services (GSI).
 *
 * Validation chain (all manual — avoids DefaultJWTClaimsVerifier constructor
 * ambiguity that caused "usar otra cuenta" tokens to fail):
 *   1. RS256 signature against Google's public JWKS
 *   2. Expiry (exp claim)
 *   3. Issuer: "accounts.google.com" OR "https://accounts.google.com"
 *   4. Audience: must contain our GOOGLE_CLIENT_ID
 *   5. Required claims present: sub, email
 */
@Slf4j
@Component
public class NimbusGoogleIdTokenVerifier implements GoogleIdTokenVerifierPort {

    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    @Value("${app.google.client-id}")
    private String expectedClientId;

    @Override
    public GoogleTokenClaims verify(String idToken) {
        try {
            // ── 1. Signature verification via Google JWKS ─────────────────────
            var jwkSource = JWKSourceBuilder
                    .create(new URL(GOOGLE_JWKS_URL))
                    .retrying(true)
                    .build();

            var keySelector = new JWSVerificationKeySelector<SecurityContext>(
                    JWSAlgorithm.RS256, jwkSource);

            // No DefaultJWTClaimsVerifier — we validate every claim manually below
            // to avoid the ambiguous constructor that rejects "usar otra cuenta" tokens.
            var processor = new DefaultJWTProcessor<SecurityContext>();
            processor.setJWSKeySelector(keySelector);
            processor.setJWTClaimsSetVerifier(null); // disable built-in verifier

            JWTClaimsSet claims = processor.process(idToken, null);

            // ── 2. Expiry ─────────────────────────────────────────────────────
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.before(new Date())) {
                throw new BusinessException("GOOGLE_TOKEN_INVALID",
                        "El token de Google ha expirado. Intenta de nuevo.");
            }

            // ── 3. Issuer — Google uses both forms; accept either ─────────────
            String issuer = claims.getIssuer();
            if (!"accounts.google.com".equals(issuer)
                    && !"https://accounts.google.com".equals(issuer)) {
                log.warn("Google ID Token rejected: unexpected issuer '{}'", issuer);
                throw new BusinessException("GOOGLE_TOKEN_INVALID",
                        "El token de Google no es válido. Intenta de nuevo.");
            }

            // ── 4. Audience — must contain our client ID ──────────────────────
            List<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(expectedClientId)) {
                log.warn("Google ID Token rejected: audience {} does not contain expected client id", audience);
                throw new BusinessException("GOOGLE_TOKEN_INVALID",
                        "El token de Google no es válido. Intenta de nuevo.");
            }

            // ── 5. Required claims ────────────────────────────────────────────
            String sub   = claims.getSubject();
            String email = claims.getStringClaim("email");
            if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
                throw new BusinessException("GOOGLE_TOKEN_INVALID",
                        "El token de Google no contiene la información requerida.");
            }

            boolean emailVerified = Boolean.TRUE.equals(
                    claims.getBooleanClaim("email_verified"));

            return new GoogleTokenClaims(sub, email, emailVerified,
                    claims.getStringClaim("name"));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google ID Token verification failed: {}", e.getMessage());
            throw new BusinessException("GOOGLE_TOKEN_INVALID",
                    "El token de Google no es válido o ha expirado. Intenta de nuevo.");
        }
    }
}
