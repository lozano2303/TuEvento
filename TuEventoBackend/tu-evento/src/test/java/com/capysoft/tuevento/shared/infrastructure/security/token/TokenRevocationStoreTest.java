package com.capysoft.tuevento.shared.infrastructure.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.capysoft.tuevento.shared.infrastructure.security.token.TokenRevocationPort.TokenRevocationUnavailableException;

@DisplayName("Access token revocation list")
class TokenRevocationStoreTest {

    private static final Instant NOW = Instant.parse("2026-10-27T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static AccessTokenClaims claims(String jti, Instant issuedAt) {
        return new AccessTokenClaims(15, "bob", "ORGANIZER", jti, issuedAt, NOW.plusSeconds(840));
    }

    @Nested
    @DisplayName("Redis")
    class Redis {

        private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        private final ValueOperations<String, String> values = mock(ValueOperations.class);
        private final RedisTokenRevocationStore store =
                new RedisTokenRevocationStore(redis, Duration.ofMinutes(15), CLOCK);

        @BeforeEach
        void setUp() {
            when(redis.opsForValue()).thenReturn(values);
        }

        @Test
        @DisplayName("revoking a user stores the cutoff in ms for one access token lifetime plus the skew")
        void revokeUserStoresCutoff() {
            store.revokeUser(15, NOW);

            verify(values).set("tuevento:revocation:user:15", String.valueOf(NOW.toEpochMilli()), Duration.ofMinutes(16));
        }

        @Test
        @DisplayName("an older revocation never moves back a later stored cutoff")
        void revokeUserKeepsLaterCutoff() {
            long later = NOW.plusSeconds(5).toEpochMilli();
            when(values.get("tuevento:revocation:user:15")).thenReturn(String.valueOf(later));

            store.revokeUser(15, NOW);

            verify(values).set("tuevento:revocation:user:15", String.valueOf(later), Duration.ofMinutes(16));
        }

        @Test
        @DisplayName("logout stores the jti until the token expires (plus the skew) and skips expired tokens")
        void revokeTokenUntilExpiry() {
            store.revokeToken("jti-1", NOW.plusSeconds(600));
            store.revokeToken("jti-2", NOW.minusSeconds(120));
            store.revokeToken(null, NOW.plusSeconds(600));

            verify(values).set("tuevento:revocation:jti:jti-1", "1", Duration.ofSeconds(660));
            verify(values, never()).set(eq("tuevento:revocation:jti:jti-2"), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("checks the user and the jti in a single Redis round trip")
        void singleRoundTrip() {
            when(values.multiGet(anyList())).thenReturn(Arrays.asList(null, null));

            assertThat(store.isRevoked(claims("jti-1", NOW.minusSeconds(60)))).isFalse();
            verify(values).multiGet(List.of("tuevento:revocation:user:15", "tuevento:revocation:jti:jti-1"));
        }

        @Test
        @DisplayName("a token issued before the cutoff is revoked; one issued after it is not")
        void comparesIssuedAtWithCutoff() {
            String cutoff = String.valueOf(NOW.toEpochMilli());
            when(values.multiGet(anyList())).thenReturn(Arrays.asList(cutoff, null));

            assertThat(store.isRevoked(claims("a", NOW.minusMillis(1)))).isTrue();
            assertThat(store.isRevoked(claims("b", NOW))).isFalse();
            assertThat(store.isRevoked(claims("c", NOW.plusSeconds(1)))).isFalse();
            assertThat(store.isRevoked(claims("d", null))).isTrue();
        }

        @Test
        @DisplayName("a jti closed by logout is revoked")
        void loggedOutToken() {
            when(values.multiGet(anyList())).thenReturn(Arrays.asList(null, "1"));

            assertThat(store.isRevoked(claims("jti-1", NOW.minusSeconds(60)))).isTrue();
        }

        @Test
        @DisplayName("fail-closed: when Redis fails, nothing is checked or revoked silently")
        void redisFailureIsUnavailable() {
            when(values.multiGet(anyList())).thenThrow(new RedisConnectionFailureException("down"));
            when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
            org.mockito.Mockito.doThrow(new RedisConnectionFailureException("down"))
                    .when(values).set(anyString(), anyString(), any(Duration.class));

            assertThatThrownBy(() -> store.isRevoked(claims("jti-1", NOW)))
                    .isInstanceOf(TokenRevocationUnavailableException.class);
            assertThatThrownBy(() -> store.revokeUser(15, NOW))
                    .isInstanceOf(TokenRevocationUnavailableException.class);
            assertThatThrownBy(() -> store.revokeToken("jti-1", NOW.plusSeconds(60)))
                    .isInstanceOf(TokenRevocationUnavailableException.class);
        }

        @Test
        @DisplayName("fail-closed: an empty Redis answer (for example inside a transaction) is not taken as not revoked")
        void nullAnswerIsUnavailable() {
            when(values.multiGet(anyList())).thenReturn(null);

            assertThatThrownBy(() -> store.isRevoked(claims("jti-1", NOW)))
                    .isInstanceOf(TokenRevocationUnavailableException.class);
        }
    }

    @Nested
    @DisplayName("In memory (only without Redis configured)")
    class Memory {

        private final InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore(CLOCK);

        @Test
        @DisplayName("same semantics: user cutoff, logout by jti, and expired tokens are forgotten")
        void sameSemantics() {
            store.revokeUser(15, NOW);
            assertThat(store.isRevoked(claims("a", NOW.minusMillis(1)))).isTrue();
            assertThat(store.isRevoked(claims("b", NOW))).isFalse();

            store.revokeToken("c", NOW.plusSeconds(60));
            store.revokeToken("old", NOW.minusSeconds(1));
            assertThat(store.isRevoked(claims("c", NOW.plusSeconds(1)))).isTrue();
            assertThat(store.isRevoked(claims("old", NOW.plusSeconds(1)))).isFalse();

            store.revokeUser(15, NOW.minusSeconds(30));
            assertThat(store.isRevoked(claims("e", NOW.minusMillis(1)))).isTrue();
        }
    }
}
