package com.capysoft.tuevento.modules.security.infrastructure.external;

import com.capysoft.tuevento.modules.security.application.port.out.GoogleIdTokenVerifierPort;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Set;

/**
 * Verifies a Google ID Token using Nimbus JOSE+JWT (transitively available via
 * spring-boot-starter-oauth2-resource-server — no extra pom dependency needed).
 *
 * Validation performed:
 *   - Signature verified against Google's public JWKS endpoint
 *   - Issuer must be "accounts.google.com" or "https://accounts.google.com"
 *   - Audience must include our GOOGLE_CLIENT_ID
 *   - Token must not be expired
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
            var jwkSource = JWKSourceBuilder
                    .create(new URL(GOOGLE_JWKS_URL))
                    .retrying(true)
                    .build();

            var keySelector = new JWSVerificationKeySelector<SecurityContext>(
                    JWSAlgorithm.RS256, jwkSource);

            var processor = new DefaultJWTProcessor<SecurityContext>();
            processor.setJWSKeySelector(keySelector);

            // Verify issuer and audience in addition to signature + expiry
            processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                    expectedClientId,          // required audience
                    new JWTClaimsSet.Builder()
                            .build(),
                    // Exact match claims — issuer checked below for both forms
                    Set.of("sub", "email")     // required claims
            ));

            JWTClaimsSet claims = processor.process(idToken, null);

            // Google uses two equivalent issuer values — accept both
            String issuer = claims.getIssuer();
            if (!"accounts.google.com".equals(issuer)
                    && !"https://accounts.google.com".equals(issuer)) {
                throw new BusinessException("GOOGLE_TOKEN_INVALID",
                        "Google ID Token has an unexpected issuer: " + issuer);
            }

            boolean emailVerified = Boolean.TRUE.equals(
                    claims.getBooleanClaim("email_verified"));

            return new GoogleTokenClaims(
                    claims.getSubject(),
                    claims.getStringClaim("email"),
                    emailVerified,
                    claims.getStringClaim("name")
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google ID Token verification failed: {}", e.getMessage());
            throw new BusinessException("GOOGLE_TOKEN_INVALID",
                    "El token de Google no es válido o ha expirado. Intenta de nuevo.");
        }
    }
}
