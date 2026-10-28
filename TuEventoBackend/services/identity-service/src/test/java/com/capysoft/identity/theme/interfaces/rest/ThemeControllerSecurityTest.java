package com.capysoft.identity.theme.interfaces.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.capysoft.common.infrastructure.security.SecurityUser;
import com.capysoft.identity.shared.infrastructure.security.SecurityConfig;
import com.capysoft.identity.shared.interfaces.advice.GlobalExceptionHandler;
import com.capysoft.identity.theme.application.dto.response.ResolvedPaletteResponse;
import com.capysoft.identity.theme.application.dto.response.ThemeResponse;
import com.capysoft.identity.theme.application.port.in.ActivateThemePort;
import com.capysoft.identity.theme.application.port.in.CustomizeThemePort;
import com.capysoft.identity.theme.application.port.in.GetActivePalettePort;
import com.capysoft.identity.theme.application.port.in.GetThemeLogPort;
import com.capysoft.identity.theme.application.port.in.GetThemesPort;
import com.capysoft.identity.theme.application.port.in.ResetCustomizationPort;

/** Mismas reglas que el monolito: el catálogo es público; el tema activo es del usuario del token. */
@WebMvcTest(ThemeController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("ThemeController security")
class ThemeControllerSecurityTest {

    private static final SecurityUser USER = new SecurityUser(7, "ana", "USER");

    @Autowired private MockMvc mockMvc;

    // Sin servidor JWKS en el test: la autenticación se inyecta con el mismo conversor que en producción
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private GetThemesPort getThemesPort;
    @MockitoBean private ActivateThemePort activateThemePort;
    @MockitoBean private GetActivePalettePort getActivePalettePort;
    @MockitoBean private CustomizeThemePort customizeThemePort;
    @MockitoBean private ResetCustomizationPort resetCustomizationPort;
    @MockitoBean private GetThemeLogPort getThemeLogPort;

    private static RequestPostProcessor as(SecurityUser user) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(user.getAlias())
                .claim("userId", user.getUserId()).claim("role", user.getRole())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return authentication(SecurityConfig.toAuthentication(jwt));
    }

    @Test
    @DisplayName("el catálogo de temas es público y respeta el idioma pedido")
    void catalogIsPublic() throws Exception {
        when(getThemesPort.getAll("en")).thenReturn(List.of(ThemeResponse.builder().id(2).name("Oscuro")
                .translatedName("Dark").languageCode("en").build()));

        mockMvc.perform(get("/api/v1/themes").param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].translatedName").value("Dark"));
    }

    @Test
    @DisplayName("sin token, el tema activo, personalizar, resetear, activar y el historial dan 401")
    void userEndpointsRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/themes/my-active")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/themes/my-active/log")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/themes/activate/2")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/themes/my-active/customize").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/themes/my-active/customize/primary")).andExpect(status().isUnauthorized());
        verifyNoInteractions(activateThemePort, getActivePalettePort, customizeThemePort,
                resetCustomizationPort, getThemeLogPort);
    }

    @Test
    @DisplayName("con token, actúa sobre el usuario del token")
    void actsOnTheTokenUser() throws Exception {
        when(activateThemePort.activate(eq(7), eq(2)))
                .thenReturn(ResolvedPaletteResponse.builder().themeId(2).themeName("Oscuro").build());
        when(getActivePalettePort.getActivePalette(7))
                .thenReturn(ResolvedPaletteResponse.builder().themeId(2).build());

        mockMvc.perform(post("/api/v1/themes/activate/2").with(as(USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.themeName").value("Oscuro"));
        mockMvc.perform(get("/api/v1/themes/my-active").with(as(USER))).andExpect(status().isOk());
        verify(activateThemePort).activate(7, 2);
    }

    @Test
    @DisplayName("un refresh token (sin rol) no sirve para la API")
    void refreshTokenIsRejected() {
        Jwt refresh = Jwt.withTokenValue("token").header("alg", "RS256").subject("ana").claim("userId", 7)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> SecurityConfig.toAuthentication(refresh))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }
}
