package com.capysoft.tuevento.modules.security.infrastructure.external;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tokens issued to the same user within the same second used to be identical (iat/exp have
 * second precision and there was no jti), which created auth_session / refresh_token rows that
 * shared a token. Every token now carries a random jti.
 */
@DisplayName("JwtTokenGenerator")
class JwtTokenGeneratorTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-characters!!";

    private JwtTokenGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new JwtTokenGenerator();
        ReflectionTestUtils.setField(generator, "secret", SECRET);
        ReflectionTestUtils.setField(generator, "accessTokenExpiration", 900_000L);
        ReflectionTestUtils.setField(generator, "refreshTokenExpiration", 604_800_000L);
        generator.init();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    @DisplayName("two access tokens for the same user in the same second are different")
    void accessTokens_sameSecond_areUnique() {
        // The pair can straddle a second boundary (the first token is slow while JJWT warms up),
        // so retry until both share the same iat; that is the case the jti must disambiguate.
        String first;
        String second;
        int attempts = 0;
        do {
            first  = generator.generateAccessToken(42, "alias", "USER");
            second = generator.generateAccessToken(42, "alias", "USER");
        } while (!claims(first).getIssuedAt().equals(claims(second).getIssuedAt()) && ++attempts < 5);

        assertThat(claims(first).getIssuedAt()).isEqualTo(claims(second).getIssuedAt());
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("two refresh tokens for the same user in the same second are different")
    void refreshTokens_sameSecond_areUnique() {
        assertThat(generator.generateRefreshToken(42)).isNotEqualTo(generator.generateRefreshToken(42));
    }

    @Test
    @DisplayName("access and refresh tokens carry a UUID jti and keep their existing claims")
    void tokens_carryUuidJti() {
        String access  = generator.generateAccessToken(42, "alias", "ORGANIZER");
        String refresh = generator.generateRefreshToken(42);

        assertThatCode(() -> UUID.fromString(claims(access).getId())).doesNotThrowAnyException();
        assertThatCode(() -> UUID.fromString(claims(refresh).getId())).doesNotThrowAnyException();
        assertThat(generator.extractUserId(access)).contains(42);
        assertThat(generator.extractRole(access)).contains("ORGANIZER");
        assertThat(generator.extractSubject(access)).contains("alias");
        assertThat(generator.isTokenValid(refresh)).isTrue();
    }
}
