package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.security.application.dto.request.GoogleAuthRequest;
import com.capysoft.tuevento.modules.security.application.dto.request.LoginRequest;
import com.capysoft.tuevento.modules.security.application.dto.request.LogoutRequest;
import com.capysoft.tuevento.modules.security.application.dto.request.RefreshTokenRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.application.dto.response.RefreshTokenResponse;
import com.capysoft.tuevento.modules.security.application.port.in.*;
import com.capysoft.tuevento.shared.domain.exception.UnauthorizedException;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El refresh token de la web viaja en una cookie httpOnly ({@code X-Auth-Mode: cookie});
 * la app móvil, sin la cabecera, sigue recibiéndolo en el cuerpo.
 */
@DisplayName("AuthController — refresh token en cookie para la web")
class AuthControllerRefreshCookieTest {

    private static final String COOKIE = "cookie";

    private final LoginPort         loginPort         = mock(LoginPort.class);
    private final LogoutPort        logoutPort        = mock(LogoutPort.class);
    private final RefreshTokenPort  refreshTokenPort  = mock(RefreshTokenPort.class);
    private final OauthLoginPort    oauthLoginPort    = mock(OauthLoginPort.class);
    private final GoogleAuthPort    googleAuthPort    = mock(GoogleAuthPort.class);

    private AuthController controller;

    private final LoginResponse login = LoginResponse.builder()
            .accessToken("access-1").refreshToken("refresh-1")
            .userId(7).alias("ana").role("USER").build();

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                mock(RegisterUserPort.class), mock(ActivateAccountPort.class), loginPort, logoutPort,
                refreshTokenPort, mock(RecoverPasswordPort.class), mock(ResetPasswordPort.class),
                oauthLoginPort, googleAuthPort, mock(RequestReactivationPort.class),
                mock(ConfirmReactivationPort.class), new RefreshTokenCookie(true, "Strict"),
                Map.of("google", "https://accounts.google.com/auth?client_id=x"),
                new LinkedHashSet<>(Set.of("http://localhost:3000/login")));
    }

    private static String setCookie(ResponseEntity<?> response) {
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    }

    @Test
    @DisplayName("login web: refresh token en cookie httpOnly y fuera del cuerpo")
    void webLoginSetsCookieAndHidesRefreshToken() {
        when(loginPort.login(any())).thenReturn(login);

        ResponseEntity<ApiResponse<LoginResponse>> response =
                controller.login(LoginRequest.builder().email("a@gmail.com").password("x").build(), COOKIE);

        assertThat(setCookie(response))
                .startsWith(RefreshTokenCookie.NAME + "=refresh-1")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=604800");
        assertThat(response.getBody().getData().getRefreshToken()).isNull();
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("access-1");
    }

    @Test
    @DisplayName("login móvil: sin cookie y con refresh token en el cuerpo")
    void mobileLoginKeepsBody() {
        when(loginPort.login(any())).thenReturn(login);

        ResponseEntity<ApiResponse<LoginResponse>> response =
                controller.login(LoginRequest.builder().email("a@gmail.com").password("x").build(), null);

        assertThat(setCookie(response)).isNull();
        assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("refresh-1");
    }

    @Test
    @DisplayName("login con Google en modo cookie también usa la cookie")
    void googleLoginUsesCookie() {
        when(googleAuthPort.authenticate(any())).thenReturn(login);

        ResponseEntity<ApiResponse<LoginResponse>> response =
                controller.googleAuth(new GoogleAuthRequest(), COOKIE);

        assertThat(setCookie(response)).startsWith(RefreshTokenCookie.NAME + "=refresh-1");
        assertThat(response.getBody().getData().getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("refresh web: lee la cookie y la rota")
    void webRefreshReadsAndRotatesCookie() {
        when(refreshTokenPort.refresh(any())).thenReturn(
                RefreshTokenResponse.builder().accessToken("access-2").refreshToken("refresh-2").build());

        ResponseEntity<ApiResponse<RefreshTokenResponse>> response = controller.refresh(null, "refresh-1", COOKIE);

        ArgumentCaptor<RefreshTokenRequest> sent = ArgumentCaptor.forClass(RefreshTokenRequest.class);
        verify(refreshTokenPort).refresh(sent.capture());
        assertThat(sent.getValue().getRefreshToken()).isEqualTo("refresh-1");
        assertThat(setCookie(response)).startsWith(RefreshTokenCookie.NAME + "=refresh-2");
        assertThat(response.getBody().getData().getRefreshToken()).isNull();
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("access-2");
    }

    @Test
    @DisplayName("refresh web: un token antiguo en el cuerpo se migra a la cookie")
    void webRefreshPrefersBodyTokenForMigration() {
        when(refreshTokenPort.refresh(any())).thenReturn(
                RefreshTokenResponse.builder().accessToken("access-2").refreshToken("refresh-2").build());

        controller.refresh(new RefreshTokenRequest("legacy"), null, COOKIE);

        ArgumentCaptor<RefreshTokenRequest> sent = ArgumentCaptor.forClass(RefreshTokenRequest.class);
        verify(refreshTokenPort).refresh(sent.capture());
        assertThat(sent.getValue().getRefreshToken()).isEqualTo("legacy");
    }

    @Test
    @DisplayName("refresh móvil: cuerpo sin cambios y la cookie se ignora")
    void mobileRefreshIgnoresCookie() {
        when(refreshTokenPort.refresh(any())).thenReturn(
                RefreshTokenResponse.builder().accessToken("access-2").refreshToken("refresh-2").build());

        ResponseEntity<ApiResponse<RefreshTokenResponse>> response =
                controller.refresh(new RefreshTokenRequest("refresh-1"), "otro", null);

        assertThat(setCookie(response)).isNull();
        assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("refresh-2");
        assertThatThrownBy(() -> controller.refresh(null, "refresh-1", null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("refresh sin token responde 401")
    void refreshWithoutTokenIsUnauthorized() {
        assertThatThrownBy(() -> controller.refresh(new RefreshTokenRequest(" "), null, COOKIE))
                .isInstanceOf(UnauthorizedException.class);
        verify(refreshTokenPort, never()).refresh(any());
    }

    @Test
    @DisplayName("logout web borra la cookie")
    void webLogoutClearsCookie() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        controller.logout(new LogoutRequest("access-1"), COOKIE, servletResponse);

        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE))
                .startsWith(RefreshTokenCookie.NAME + "=")
                .contains("Max-Age=0");
        verify(logoutPort).logout(any());
    }

    @Test
    @DisplayName("logout móvil no toca cookies")
    void mobileLogoutSetsNoCookie() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        controller.logout(new LogoutRequest("access-1"), null, servletResponse);

        assertThat(servletResponse.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("callback OAuth web: tokens fuera de la URL y refresh token en cookie")
    void oauthRedirectUsesCookieInsteadOfUrl() {
        when(oauthLoginPort.login(anyString(), anyString())).thenReturn(login);

        ResponseEntity<?> response = controller.oauthCallback("google", "code", null, null, null);

        String location = response.getHeaders().getLocation().toString();
        assertThat(location).startsWith("http://localhost:3000/login?oauth=true&userID=7")
                .doesNotContain("access-1").doesNotContain("refresh-1");
        assertThat(setCookie(response)).startsWith(RefreshTokenCookie.NAME + "=refresh-1");
    }

    @Test
    @DisplayName("callback OAuth móvil (json): sin cookie y con tokens en el cuerpo")
    void oauthJsonKeepsBody() {
        when(oauthLoginPort.login(anyString(), anyString())).thenReturn(login);

        ResponseEntity<?> response = controller.oauthCallback("google", "code", null, null, "json");

        assertThat(setCookie(response)).isNull();
    }
}
