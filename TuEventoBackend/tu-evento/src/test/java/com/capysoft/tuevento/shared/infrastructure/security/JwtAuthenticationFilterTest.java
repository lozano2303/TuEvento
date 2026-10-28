package com.capysoft.tuevento.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenClaims;
import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenVerifier;
import com.capysoft.tuevento.shared.infrastructure.security.token.TokenRevocationPort;
import com.capysoft.tuevento.shared.infrastructure.security.token.TokenRevocationPort.TokenRevocationUnavailableException;
import com.capysoft.tuevento.shared.infrastructure.security.token.InMemoryTokenRevocationStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "jwt-token";
    private static final Instant NOW = Instant.parse("2026-10-27T15:00:00Z");
    private static final Instant ISSUED_AT = NOW.minusSeconds(60);

    @Mock private AccessTokenVerifier tokenVerifier;
    @Mock private FilterChain filterChain;

    private final InMemoryTokenRevocationStore revocations =
            new InMemoryTokenRevocationStore(Clock.fixed(NOW, ZoneOffset.UTC));
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenVerifier, revocations, new ObjectMapper());
        request = new MockHttpServletRequest("GET", "/api/v1/events");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void tokenWithRole(String role) {
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.of(
                new AccessTokenClaims(15, "bob", role, "jti-1", ISSUED_AT, NOW.plusSeconds(840))));
    }

    private String authenticatedRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse(null);
    }

    @Test
    @DisplayName("token vigente → autentica con el rol del token, sin consultar la BD")
    void validToken_authenticatesWithTokenRole() throws Exception {
        tokenWithRole("ORGANIZER");

        filter.doFilter(request, response, filterChain);

        assertThat(authenticatedRole()).isEqualTo("ORGANIZER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("rol revocado después de emitir el token → la petición sigue como anónima (401 en lo protegido)")
    void tokenIssuedBeforeUserRevocation_isIgnored() throws Exception {
        tokenWithRole("ORGANIZER");
        revocations.revokeUser(15, ISSUED_AT.plusMillis(1));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("un token emitido después de la revocación (tras renovar) sí vale")
    void tokenIssuedAfterUserRevocation_isAccepted() throws Exception {
        tokenWithRole("USER");
        revocations.revokeUser(15, ISSUED_AT.minusMillis(1));

        filter.doFilter(request, response, filterChain);

        assertThat(authenticatedRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("token cerrado con logout → no autentica")
    void loggedOutToken_isIgnored() throws Exception {
        tokenWithRole("USER");
        revocations.revokeToken("jti-1", NOW.plusSeconds(840));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("lista de revocación caída → 503 y la petición no sigue (fail-closed)")
    void revocationStoreDown_rejectsWith503() throws Exception {
        tokenWithRole("ORGANIZER");
        TokenRevocationPort down = mock(TokenRevocationPort.class);
        when(down.isRevoked(any())).thenThrow(new TokenRevocationUnavailableException("redis down", null));
        filter = new JwtAuthenticationFilter(tokenVerifier, down, new ObjectMapper());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("AUTH_UNAVAILABLE");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("sin token no consulta la lista de revocación: lo público sigue funcionando aunque Redis caiga")
    void noToken_skipsRevocationCheck() throws Exception {
        TokenRevocationPort down = mock(TokenRevocationPort.class);
        filter = new JwtAuthenticationFilter(tokenVerifier, down, new ObjectMapper());
        MockHttpServletRequest anonymous = new MockHttpServletRequest("GET", "/api/v1/events");

        filter.doFilter(anonymous, response, filterChain);

        verify(down, never()).isRevoked(any());
        verify(filterChain).doFilter(anonymous, response);
    }

    @Test
    @DisplayName("token inválido → sigue la cadena sin autenticar")
    void invalidToken_doesNotAuthenticate() throws Exception {
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("un refresh token (sin rol) no autentica la petición, que sigue como anónima")
    void tokenWithoutRole_isNotAnAccessToken() throws Exception {
        // El verificador descarta el refresh token (sin rol): no hay claims de access token
        when(tokenVerifier.verify(TOKEN)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
