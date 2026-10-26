package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.application.port.in.*;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GET /api/v1/auth/oauth/{provider}/callback: errors go back to the frontend login page with
 * oauth_error (browser) or as 400 with the code (mobile, response_format=json), never as a 500.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — OAuth callback")
class AuthControllerOauthCallbackTest {

    private static final String LOGIN_URI = "https://app.example.com/login";
    private static final String STATE     = state("csrf-123", LOGIN_URI);

    @Mock private OauthLoginPort oauthLoginPort;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                mock(RegisterUserPort.class), mock(ActivateAccountPort.class), mock(LoginPort.class),
                mock(LogoutPort.class), mock(RefreshTokenPort.class), mock(RecoverPasswordPort.class),
                mock(ResetPasswordPort.class), oauthLoginPort, mock(GoogleAuthPort.class),
                mock(RequestReactivationPort.class), mock(ConfirmReactivationPort.class),
                new RefreshTokenCookie(true, "Strict"),
                Map.of("google", "https://accounts.google.com/o/oauth2/v2/auth?client_id=1"),
                Set.of(LOGIN_URI));
    }

    /** Composite state as built by GET /oauth/{provider}: {"s":csrf,"r":frontendUri}. */
    private static String state(String csrf, String frontendUri) {
        return "{\"s\":\"" + csrf + "\",\"r\":\"" + frontendUri + "\"}";
    }

    private static String location(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        return response.getHeaders().getLocation().toString();
    }

    @Nested
    @DisplayName("Browser (302)")
    class Browser {

        @Test
        @DisplayName("user cancels (error=access_denied, no code) → /login?oauth_error=OAUTH_CANCELLED, no 500")
        void cancelled_redirectsWithError() {
            ResponseEntity<?> response = controller.oauthCallback("google", null, STATE, "access_denied", null);

            assertThat(location(response)).isEqualTo(LOGIN_URI + "?oauth_error=OAUTH_CANCELLED&state=csrf-123");
            verifyNoInteractions(oauthLoginPort);
        }

        @Test
        @DisplayName("no code and no error → OAUTH_FAILED")
        void missingCode_redirectsWithFailed() {
            ResponseEntity<?> response = controller.oauthCallback("google", "", STATE, null, null);

            assertThat(location(response)).startsWith(LOGIN_URI + "?oauth_error=OAUTH_FAILED");
            verifyNoInteractions(oauthLoginPort);
        }

        @Test
        @DisplayName("sign-in rejected (ACCOUNT_BLOCKED) → redirect carries that code")
        void businessError_redirectsWithItsCode() {
            when(oauthLoginPort.login("google", "code-1"))
                    .thenThrow(new BusinessException("ACCOUNT_BLOCKED", "blocked"));

            ResponseEntity<?> response = controller.oauthCallback("google", "code-1", STATE, null, null);

            assertThat(location(response)).startsWith(LOGIN_URI + "?oauth_error=ACCOUNT_BLOCKED");
        }

        @Test
        @DisplayName("unexpected failure → OAUTH_FAILED, no detail in the URL")
        void unexpectedError_redirectsWithFailed() {
            when(oauthLoginPort.login(any(), any())).thenThrow(new IllegalStateException("graph.facebook.com said: secret"));

            String location = location(controller.oauthCallback("google", "code-1", STATE, null, null));

            assertThat(location).startsWith(LOGIN_URI + "?oauth_error=OAUTH_FAILED").doesNotContain("secret");
        }

        @Test
        @DisplayName("state pointing to an unlisted URI → falls back to the whitelisted login page")
        void unlistedStateUri_usesWhitelist() {
            String evilState = state("x", "https://evil.example.com/login");

            String location = location(controller.oauthCallback("google", null, evilState, "access_denied", null));

            assertThat(location).startsWith(LOGIN_URI + "?oauth_error=");
        }

        @Test
        @DisplayName("success → /login?oauth=true with the original state; refresh token only in the httpOnly cookie")
        void success_redirectsWithCookie() {
            when(oauthLoginPort.login("google", "code-1")).thenReturn(LoginResponse.builder()
                    .accessToken("access").refreshToken("refresh").userId(5).alias("ana").role("USER").build());

            ResponseEntity<?> response = controller.oauthCallback("google", "code-1", STATE, null, null);
            String location = location(response);

            assertThat(location).startsWith(LOGIN_URI + "?oauth=true&userID=5")
                    .contains("&state=" + URLEncoder.encode("csrf-123", StandardCharsets.UTF_8))
                    .doesNotContain("access").doesNotContain("refresh");
            assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                    .startsWith(RefreshTokenCookie.NAME + "=refresh").contains("HttpOnly");
        }

        @Test
        @DisplayName("an error redirect sets no session cookie")
        void error_setsNoCookie() {
            ResponseEntity<?> response = controller.oauthCallback("google", null, STATE, "access_denied", null);

            assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();
        }
    }

    @Nested
    @DisplayName("Mobile (response_format=json)")
    class Mobile {

        @Test
        @DisplayName("cancelled → 400 with OAUTH_CANCELLED")
        void cancelled_badRequest() {
            ResponseEntity<?> response = controller.oauthCallback("google", null, null, "access_denied", "json");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(((ApiResponse<?>) response.getBody()).getCode()).isEqualTo("OAUTH_CANCELLED");
        }

        @Test
        @DisplayName("sign-in rejected → 400 with the business code")
        void businessError_badRequest() {
            when(oauthLoginPort.login(any(), any()))
                    .thenThrow(new BusinessException("ACCOUNT_DEACTIVATED", "deactivated"));

            ResponseEntity<?> response = controller.oauthCallback("google", "code-1", null, null, "json");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(((ApiResponse<?>) response.getBody()).getCode()).isEqualTo("ACCOUNT_DEACTIVATED");
        }
    }
}
