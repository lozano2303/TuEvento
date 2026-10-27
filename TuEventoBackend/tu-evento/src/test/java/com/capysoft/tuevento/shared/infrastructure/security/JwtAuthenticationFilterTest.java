package com.capysoft.tuevento.shared.infrastructure.security;

import com.capysoft.tuevento.modules.security.application.port.out.CurrentRolePort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "jwt-token";

    @Mock private TokenGeneratorPort tokenGenerator;
    @Mock private CurrentRolePort currentRolePort;
    @Mock private FilterChain filterChain;
    @InjectMocks private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/v1/events");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void tokenWithRole(String role) {
        when(tokenGenerator.isTokenValid(TOKEN)).thenReturn(true);
        when(tokenGenerator.extractUserId(TOKEN)).thenReturn(Optional.of(15));
        when(tokenGenerator.extractSubject(TOKEN)).thenReturn(Optional.of("bob"));
        when(tokenGenerator.extractRole(TOKEN)).thenReturn(Optional.of(role));
    }

    private String authenticatedRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse(null);
    }

    @Test
    @DisplayName("token ORGANIZER con rol vigente en BD → mantiene ORGANIZER")
    void organizerToken_stillOrganizer() throws Exception {
        tokenWithRole("ORGANIZER");
        when(currentRolePort.findRoleCode(15)).thenReturn(Optional.of("ORGANIZER"));

        filter.doFilter(request, response, filterChain);

        assertThat(authenticatedRole()).isEqualTo("ORGANIZER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("token ORGANIZER con rol revocado en BD → usa el rol de la BD")
    void organizerToken_revokedInDatabase_usesDatabaseRole() throws Exception {
        tokenWithRole("ORGANIZER");
        when(currentRolePort.findRoleCode(15)).thenReturn(Optional.of("USER"));

        filter.doFilter(request, response, filterChain);

        assertThat(authenticatedRole()).isEqualTo("USER");
    }

    @Test
    @DisplayName("fallo al consultar la BD → conserva el rol del token")
    void organizerToken_databaseFailure_keepsTokenRole() throws Exception {
        tokenWithRole("ORGANIZER");
        when(currentRolePort.findRoleCode(15)).thenThrow(new RuntimeException("DB down"));

        filter.doFilter(request, response, filterChain);

        assertThat(authenticatedRole()).isEqualTo("ORGANIZER");
    }

    @Test
    @DisplayName("token con otro rol → no consulta la BD")
    void nonOrganizerToken_skipsDatabase() throws Exception {
        tokenWithRole("USER");

        filter.doFilter(request, response, filterChain);

        assertThat(authenticatedRole()).isEqualTo("USER");
        verifyNoInteractions(currentRolePort);
    }

    @Test
    @DisplayName("token inválido → sigue la cadena sin autenticar")
    void invalidToken_doesNotAuthenticate() throws Exception {
        when(tokenGenerator.isTokenValid(TOKEN)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("un refresh token (sin rol) no autentica la petición, que sigue como anónima")
    void tokenWithoutRole_isNotAnAccessToken() throws Exception {
        when(tokenGenerator.isTokenValid(TOKEN)).thenReturn(true);
        when(tokenGenerator.extractUserId(TOKEN)).thenReturn(Optional.of(15));
        when(tokenGenerator.extractRole(TOKEN)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
