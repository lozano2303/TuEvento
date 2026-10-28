package com.capysoft.identity.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

@DisplayName("Lectura del Bearer en identity-service")
class BearerTokenResolverTest {

    private final BearerTokenResolver resolver = SecurityConfig.bearerTokenResolver();

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        request.addHeader("Authorization", "Bearer abc.def.ghi");
        return request;
    }

    @Test
    @DisplayName("login, renovación y demás flujos de /auth ignoran un token viejo en la cabecera")
    void authFlowsIgnoreTheHeader() {
        assertThat(resolver.resolve(request("/api/v1/auth/login"))).isNull();
        assertThat(resolver.resolve(request("/api/v1/auth/refresh"))).isNull();
        assertThat(resolver.resolve(request("/api/v1/auth/register"))).isNull();
    }

    @Test
    @DisplayName("el logout y el resto de la API sí leen el token")
    void logoutAndApiReadTheHeader() {
        assertThat(resolver.resolve(request("/api/v1/auth/logout"))).isEqualTo("abc.def.ghi");
        assertThat(resolver.resolve(request("/api/v1/users/change-password"))).isEqualTo("abc.def.ghi");
        assertThat(resolver.resolve(request("/api/v1/themes/my-active"))).isEqualTo("abc.def.ghi");
    }
}
