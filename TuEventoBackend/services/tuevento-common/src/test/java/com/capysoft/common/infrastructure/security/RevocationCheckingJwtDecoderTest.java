package com.capysoft.common.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

@DisplayName("Revocación de tokens en los microservicios")
class RevocationCheckingJwtDecoderTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-10-27T15:00:00.250Z");

    private final JwtDecoder delegate = mock(JwtDecoder.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final RevocationCheckingJwtDecoder decoder =
            new RevocationCheckingJwtDecoder(delegate, new RedisTokenRevocationChecker(redis));

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
    }

    private Jwt jwt(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("tok").header("alg", "RS256").jti("jti-1")
                .issuedAt(Instant.parse("2026-10-27T15:00:00Z")).expiresAt(ISSUED_AT.plusSeconds(900))
                .claim("iatMs", ISSUED_AT.toEpochMilli()).claim("role", "ORGANIZER");
        if (userId != null) {
            builder.claim("userId", userId);
        }
        Jwt jwt = builder.build();
        when(delegate.decode("tok")).thenReturn(jwt);
        return jwt;
    }

    @Test
    @DisplayName("un token no revocado pasa, consultando usuario y jti en una sola ida a Redis")
    void validTokenPasses() {
        Jwt jwt = jwt(15);
        when(values.multiGet(anyList())).thenReturn(Arrays.asList(null, null));

        assertThat(decoder.decode("tok")).isSameAs(jwt);
        verify(values).multiGet(List.of("tuevento:revocation:user:15", "tuevento:revocation:jti:jti-1"));
    }

    @Test
    @DisplayName("emitido antes del corte del usuario (comparando en ms con iatMs) → BadJwtException (401)")
    void revokedByUserCutoff() {
        jwt(15);
        when(values.multiGet(anyList())).thenReturn(Arrays.asList(String.valueOf(ISSUED_AT.toEpochMilli() + 1), null));

        assertThatThrownBy(() -> decoder.decode("tok")).isInstanceOf(BadJwtException.class);
    }

    @Test
    @DisplayName("emitido después del corte (tras renovar) → pasa")
    void issuedAfterCutoffPasses() {
        jwt(15);
        when(values.multiGet(anyList())).thenReturn(Arrays.asList(String.valueOf(ISSUED_AT.toEpochMilli() - 1), null));

        assertThat(decoder.decode("tok")).isNotNull();
    }

    @Test
    @DisplayName("cerrado con logout (jti) → BadJwtException (401)")
    void revokedByLogout() {
        jwt(15);
        when(values.multiGet(anyList())).thenReturn(Arrays.asList(null, "1"));

        assertThatThrownBy(() -> decoder.decode("tok")).isInstanceOf(BadJwtException.class);
    }

    @Test
    @DisplayName("Redis caído → RevocationUnavailableException (fail-closed)")
    void redisDownIsUnavailable() {
        jwt(15);
        when(values.multiGet(anyList())).thenThrow(new RedisConnectionFailureException("down"));

        assertThatThrownBy(() -> decoder.decode("tok"))
                .isInstanceOf(RevocationCheckingJwtDecoder.RevocationUnavailableException.class);
    }

    @Test
    @DisplayName("un token sin userId (refresh) no consulta Redis: lo rechaza el conversor del servicio")
    void tokenWithoutUserIdSkipsRedis() {
        jwt(null);

        assertThat(decoder.decode("tok")).isNotNull();
        verifyNoInteractions(values);
    }

    @Test
    @DisplayName("el entry point responde 503 si falló la lista de revocación y 401 en lo demás")
    void entryPointDistinguishesUnavailable() throws Exception {
        RevocationAwareAuthenticationEntryPoint entryPoint = new RevocationAwareAuthenticationEntryPoint();

        MockHttpServletResponse unavailable = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), unavailable, new AuthenticationServiceException("x",
                new RevocationCheckingJwtDecoder.RevocationUnavailableException(new RuntimeException("down"))));
        assertThat(unavailable.getStatus()).isEqualTo(503);
        assertThat(unavailable.getContentAsString()).contains("AUTH_UNAVAILABLE");

        MockHttpServletResponse revoked = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), revoked, new InvalidBearerTokenException("revoked"));
        assertThat(revoked.getStatus()).isEqualTo(401);
    }
}
