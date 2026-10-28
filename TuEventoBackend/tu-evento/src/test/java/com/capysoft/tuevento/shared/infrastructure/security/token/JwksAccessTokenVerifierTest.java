package com.capysoft.tuevento.shared.infrastructure.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@DisplayName("JwksAccessTokenVerifier")
class JwksAccessTokenVerifierTest {

    private static final Instant ISSUED = Instant.parse("2026-10-27T15:00:00Z");

    private final JwtDecoder decoder = mock(JwtDecoder.class);
    private final JwksAccessTokenVerifier verifier = new JwksAccessTokenVerifier(decoder);

    private static Jwt.Builder accessToken() {
        return Jwt.withTokenValue("token").header("alg", "RS256")
                .subject("ana").jti("jti-1").issuedAt(ISSUED).expiresAt(ISSUED.plusSeconds(900))
                .claim("userId", 7).claim("role", "ORGANIZER");
    }

    @Test
    @DisplayName("reads the user, role, jti and the millisecond issue time of an access token")
    void readsAccessTokenClaims() {
        when(decoder.decode("token")).thenReturn(accessToken().claim("iatMs", ISSUED.toEpochMilli() + 250).build());

        assertThat(verifier.verify("token")).contains(new AccessTokenClaims(7, "ana", "ORGANIZER", "jti-1",
                ISSUED.plusMillis(250), ISSUED.plusSeconds(900)));
    }

    @Test
    @DisplayName("falls back to the standard iat claim and an empty alias when they are missing")
    void fallsBackToStandardIssuedAt() {
        when(decoder.decode("token")).thenReturn(accessToken().subject(null).build());

        assertThat(verifier.verify("token")).hasValueSatisfying(claims -> {
            assertThat(claims.issuedAt()).isEqualTo(ISSUED);
            assertThat(claims.alias()).isEmpty();
        });
    }

    @Test
    @DisplayName("rejects tokens the decoder refuses and tokens without a role (refresh tokens)")
    void rejectsInvalidAndRefreshTokens() {
        when(decoder.decode("bad")).thenThrow(new BadJwtException("bad signature"));
        when(decoder.decode("refresh")).thenReturn(accessToken().claims(claims -> claims.remove("role")).build());
        when(decoder.decode("blank")).thenReturn(accessToken().claim("role", " ").build());
        when(decoder.decode("anonymous")).thenReturn(accessToken().claims(claims -> claims.remove("userId")).build());

        assertThat(verifier.verify("bad")).isEmpty();
        assertThat(verifier.verify("refresh")).isEmpty();
        assertThat(verifier.verify("blank")).isEmpty();
        assertThat(verifier.verify("anonymous")).isEmpty();
    }
}
