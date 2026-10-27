package com.capysoft.tuevento.shared.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;

@DisplayName("JwtHandshakeInterceptor — solo access tokens abren el /ws")
class JwtHandshakeInterceptorTest {

    private final TokenGeneratorPort tokens = mock(TokenGeneratorPort.class);
    private final JwtHandshakeInterceptor interceptor = new JwtHandshakeInterceptor(tokens);

    private boolean handshake(String query, Map<String, Object> attributes, MockHttpServletResponse servletResponse) throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws/1/abc/websocket");
        servletRequest.setQueryString(query);
        return interceptor.beforeHandshake(new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse), mock(WebSocketHandler.class), attributes);
    }

    @Test
    @DisplayName("un access token abre la sesión con su userId")
    void accessToken_isAccepted() throws Exception {
        when(tokens.isTokenValid("acc")).thenReturn(true);
        when(tokens.extractRole("acc")).thenReturn(Optional.of("USER"));
        when(tokens.extractUserId("acc")).thenReturn(Optional.of(7));
        Map<String, Object> attributes = new HashMap<>();

        assertThat(handshake("token=acc", attributes, new MockHttpServletResponse())).isTrue();
        assertThat(attributes).containsEntry("userId", 7);
    }

    @Test
    @DisplayName("un refresh token (sin rol) se rechaza con 401")
    void refreshToken_isRejected() throws Exception {
        when(tokens.isTokenValid("ref")).thenReturn(true);
        when(tokens.extractRole("ref")).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(handshake("token=ref", new HashMap<>(), response)).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("sin token o con un token inválido se rechaza")
    void missingOrInvalidToken_isRejected() throws Exception {
        when(tokens.isTokenValid("bad")).thenReturn(false);

        assertThat(handshake(null, new HashMap<>(), new MockHttpServletResponse())).isFalse();
        assertThat(handshake("token=bad", new HashMap<>(), new MockHttpServletResponse())).isFalse();
    }
}
