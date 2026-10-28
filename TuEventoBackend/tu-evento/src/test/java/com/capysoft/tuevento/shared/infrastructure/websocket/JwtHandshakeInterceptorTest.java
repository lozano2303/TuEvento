package com.capysoft.tuevento.shared.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenClaims;
import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenVerifier;
import com.capysoft.tuevento.shared.infrastructure.security.token.TokenRevocationPort;
import com.capysoft.tuevento.shared.infrastructure.security.token.InMemoryTokenRevocationStore;

@DisplayName("JwtHandshakeInterceptor — solo access tokens abren el /ws")
class JwtHandshakeInterceptorTest {

    private static final Instant NOW = Instant.parse("2026-10-27T15:00:00Z");

    private final AccessTokenVerifier tokens = mock(AccessTokenVerifier.class);
    private final InMemoryTokenRevocationStore revocations =
            new InMemoryTokenRevocationStore(Clock.fixed(NOW, ZoneOffset.UTC));
    private final JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(tokens, revocations);

    private void accessToken(String token) {
        when(tokens.verify(token)).thenReturn(Optional.of(
                new AccessTokenClaims(7, "org", "ORGANIZER", "jti-7", NOW.minusSeconds(60), NOW.plusSeconds(840))));
    }

    private boolean handshake(String query, Map<String, Object> attributes, MockHttpServletResponse servletResponse) throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/1/abc/websocket");
        servletRequest.setQueryString(query);
        return interceptor.beforeHandshake(new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse), mock(WebSocketHandler.class), attributes);
    }

    @Test
    @DisplayName("un access token abre la sesión con su userId")
    void accessToken_isAccepted() throws Exception {
        when(tokens.verify("acc")).thenReturn(Optional.of(
                new AccessTokenClaims(7, "ana", "USER", null, NOW.minusSeconds(60), NOW.plusSeconds(840))));
        Map<String, Object> attributes = new HashMap<>();

        assertThat(handshake("token=acc", attributes, new MockHttpServletResponse())).isTrue();
        assertThat(attributes).containsEntry("userId", 7);
    }

    @Test
    @DisplayName("un refresh token (sin rol) se rechaza con 401")
    void refreshToken_isRejected() throws Exception {
        // El verificador descarta el refresh token (sin rol)
        when(tokens.verify("ref")).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(handshake("token=ref", new HashMap<>(), response)).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("un token revocado (rol quitado o logout) no abre el /ws")
    void revokedToken_isRejected() throws Exception {
        accessToken("acc");
        revocations.revokeUser(7, NOW.minusSeconds(30));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(handshake("token=acc", new HashMap<>(), response)).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("si la lista de revocación no responde, el handshake se rechaza (fail-closed)")
    void revocationStoreDown_isRejected() throws Exception {
        accessToken("acc");
        TokenRevocationPort down = mock(TokenRevocationPort.class);
        when(down.isRevoked(any())).thenThrow(new TokenRevocationPort.TokenRevocationUnavailableException("down", null));
        JwtHandshakeInterceptor strict = new JwtHandshakeInterceptor(tokens, down);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/1/abc/websocket");
        servletRequest.setQueryString("token=acc");

        assertThat(strict.beforeHandshake(new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(new MockHttpServletResponse()), mock(WebSocketHandler.class),
                new HashMap<>())).isFalse();
    }

    @Test
    @DisplayName("sin token o con un token inválido se rechaza")
    void missingOrInvalidToken_isRejected() throws Exception {
        when(tokens.verify("bad")).thenReturn(Optional.empty());

        assertThat(handshake(null, new HashMap<>(), new MockHttpServletResponse())).isFalse();
        assertThat(handshake("token=bad", new HashMap<>(), new MockHttpServletResponse())).isFalse();
    }
}
