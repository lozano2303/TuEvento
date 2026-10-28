package com.capysoft.geolocation.language.interfaces.rest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.capysoft.common.infrastructure.security.SecurityUser;
import com.capysoft.geolocation.infrastructure.security.SecurityConfig;
import com.capysoft.geolocation.interfaces.advice.GlobalExceptionHandler;
import com.capysoft.geolocation.language.application.dto.response.LanguageResponse;
import com.capysoft.geolocation.language.application.port.in.CreateLanguageUseCase;
import com.capysoft.geolocation.language.application.port.in.GetLanguagesUseCase;
import com.capysoft.geolocation.language.application.port.in.UpdateLanguageUseCase;

/**
 * Los idiomas activos y un idioma por id se leen sin sesión; administrarlos exige ADMIN, igual
 * que en el monolito.
 */
@WebMvcTest(LanguageController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("LanguageController security")
class LanguageControllerSecurityTest {

    private static final SecurityUser USER = new SecurityUser(5, "ana", "USER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private CreateLanguageUseCase createLanguageUseCase;
    @MockitoBean private GetLanguagesUseCase getLanguagesUseCase;
    @MockitoBean private UpdateLanguageUseCase updateLanguageUseCase;

    private static RequestPostProcessor as(SecurityUser user) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(user.getAlias())
                .claim("userId", user.getUserId()).claim("role", user.getRole())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return authentication(SecurityConfig.toAuthentication(jwt));
    }

    @Test
    @DisplayName("idiomas activos e idioma por id: públicos; uno inexistente da 404")
    void publicReads() throws Exception {
        LanguageResponse english = LanguageResponse.builder().languageId(2).code("en").name("English").build();
        when(getLanguagesUseCase.getAllActiveLanguages()).thenReturn(List.of(english));
        when(getLanguagesUseCase.getLanguageById(2)).thenReturn(Optional.of(english));
        when(getLanguagesUseCase.getLanguageById(9)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/languages")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("en"));
        mockMvc.perform(get("/api/v1/languages/2")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/languages/9")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("administrar idiomas: sin sesión 401, USER 403, ADMIN 200")
    void adminOnly() throws Exception {
        when(getLanguagesUseCase.getAllLanguages()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/languages")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/admin/languages/2/activate").with(as(USER))).andExpect(status().isForbidden());
        verifyNoInteractions(updateLanguageUseCase);
        mockMvc.perform(get("/api/v1/admin/languages").with(as(ADMIN))).andExpect(status().isOk());
    }
}
