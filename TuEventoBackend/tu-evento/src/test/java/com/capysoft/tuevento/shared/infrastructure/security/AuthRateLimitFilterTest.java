package com.capysoft.tuevento.shared.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequestWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthRateLimitFilter — límite por IP en endpoints de autenticación")
class AuthRateLimitFilterTest {

    private static final String LOGIN = "/api/v1/auth/login";

    @Mock ObjectProvider<StringRedisTemplate> redisProvider;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;

    private AuthRateLimitFilter filter;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = newFilter(true, 0);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    private AuthRateLimitFilter newFilter(boolean enabled, int trustedProxies) {
        return new AuthRateLimitFilter(redisProvider, new ObjectMapper(), enabled, 3, trustedProxies);
    }

    private static MockHttpServletRequest post(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr("10.0.0.1");
        return request;
    }

    private void redisReturns(long count) {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(count);
    }

    @Test
    @DisplayName("la primera petición pasa y abre la ventana de un minuto")
    void firstRequestPassesAndStartsWindow() throws Exception {
        redisReturns(1);

        filter.doFilter(post(LOGIN), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(redis).expire(AuthRateLimitFilter.KEY_PREFIX + LOGIN + ":10.0.0.1", Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("al superar el límite responde 429 con Retry-After")
    void overLimitReturns429() throws Exception {
        redisReturns(4);
        when(redis.getExpire(anyString())).thenReturn(42L);

        filter.doFilter(post(LOGIN), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
        assertThat(response.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    @DisplayName("si Redis no devuelve TTL usa la ventana completa")
    void missingTtlFallsBackToWindow() throws Exception {
        redisReturns(4);
        when(redis.getExpire(anyString())).thenReturn(-1L);

        filter.doFilter(post(LOGIN), response, chain);

        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    @DisplayName("con dos proxies toma la penúltima IP de X-Forwarded-For e ignora las inventadas")
    void picksClientIpBehindTrustedProxies() {
        MockHttpServletRequest request = post(LOGIN);
        request.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.7 , 172.18.0.5");

        assertThat(newFilter(true, 2).resolveClientIp(request)).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("lee X-Forwarded-For de la petición original aunque un wrapper lo oculte")
    void readsHeaderFromOriginalRequest() {
        MockHttpServletRequest request = post(LOGIN);
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        HttpServletRequestWrapper stripped = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) { return null; }
        };

        assertThat(newFilter(true, 1).resolveClientIp(stripped)).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("con menos entradas que proxies usa la primera")
    void shortForwardedForUsesFirstEntry() {
        MockHttpServletRequest request = post(LOGIN);
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertThat(newFilter(true, 2).resolveClientIp(request)).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("sin proxies de confianza o sin cabecera usa la IP remota")
    void fallsBackToRemoteAddr() throws Exception {
        MockHttpServletRequest spoofed = post(LOGIN);
        spoofed.addHeader("X-Forwarded-For", "6.6.6.6");
        assertThat(filter.resolveClientIp(spoofed)).isEqualTo("10.0.0.1");

        filter = newFilter(true, 1);
        redisReturns(2);

        filter.doFilter(post(LOGIN), response, chain);

        verify(values).increment(AuthRateLimitFilter.KEY_PREFIX + LOGIN + ":10.0.0.1");
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("si Redis falla deja pasar la petición")
    void failsOpenWhenRedisFails() throws Exception {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        filter.doFilter(post(LOGIN), response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("sin Redis configurado deja pasar la petición")
    void passesWithoutRedis() throws Exception {
        when(redisProvider.getIfAvailable()).thenReturn(null);

        filter.doFilter(post(LOGIN), response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("no limita otros endpoints, otros métodos ni con el filtro desactivado")
    void skipsUnlimitedRequests() throws Exception {
        filter.doFilter(post("/api/v1/events"), response, new MockFilterChain());
        filter.doFilter(new MockHttpServletRequest("GET", LOGIN), response, new MockFilterChain());
        newFilter(false, 0).doFilter(post(LOGIN), response, new MockFilterChain());

        verify(redisProvider, never()).getIfAvailable();
    }
}
