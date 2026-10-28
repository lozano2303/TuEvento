package com.capysoft.common.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
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
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;

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
    @DisplayName("por la cadena real de Spring: Redis caído → 503 AUTH_UNAVAILABLE; token revocado → 401")
    void filterChainAnswers503WhenRevocationIsUnavailable() throws Exception {
        jwt(15);
        when(values.multiGet(anyList())).thenThrow(new RedisConnectionFailureException("down"));
        MockHttpServletResponse unavailable = authenticate();
        assertThat(unavailable.getStatus()).isEqualTo(503);
        assertThat(unavailable.getContentAsString()).contains("AUTH_UNAVAILABLE");

        doReturn(Arrays.asList(null, "1")).when(values).multiGet(anyList());
        MockHttpServletResponse revoked = authenticate();
        assertThat(revoked.getStatus()).isEqualTo(401);
        assertThat(revoked.getHeader("WWW-Authenticate")).contains("invalid_token");
    }

    /** Igual que lo monta oauth2ResourceServer().authenticationEntryPoint(...) en cada servicio. */
    private MockHttpServletResponse authenticate() throws Exception {
        BearerTokenAuthenticationFilter filter =
                new BearerTokenAuthenticationFilter(new ProviderManager(new JwtAuthenticationProvider(decoder)));
        filter.setAuthenticationFailureHandler(
                new AuthenticationEntryPointFailureHandler(new RevocationAwareAuthenticationEntryPoint()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/files");
        request.addHeader("Authorization", "Bearer tok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
