package com.capysoft.identity.profile.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
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
import com.capysoft.identity.profile.application.dto.response.ProfileResponse;
import com.capysoft.identity.profile.application.port.in.CreateProfilePort;
import com.capysoft.identity.profile.application.port.in.GetProfileByUserIdPort;
import com.capysoft.identity.profile.application.port.in.GetProfilePort;
import com.capysoft.identity.profile.application.port.in.UpdateProfilePort;
import com.capysoft.identity.shared.infrastructure.security.SecurityConfig;
import com.capysoft.identity.shared.interfaces.advice.GlobalExceptionHandler;

/** El perfil público se lee sin sesión; crearlo o editarlo, solo su dueño o un ADMIN. */
@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("ProfileController security (identity-service)")
class ProfileControllerSecurityTest {

    private static final SecurityUser ANA = new SecurityUser(7, "ana", "USER");
    private static final SecurityUser LUIS = new SecurityUser(8, "luis", "USER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");
    private static final String UPDATE_BODY = "{\"fullName\":\"Nombre Cambiado\"}";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private CreateProfilePort createProfilePort;
    @MockitoBean private UpdateProfilePort updateProfilePort;
    @MockitoBean private GetProfilePort getProfilePort;
    @MockitoBean private GetProfileByUserIdPort getProfileByUserIdPort;

    private static RequestPostProcessor as(SecurityUser user) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(user.getAlias())
                .claim("userId", user.getUserId()).claim("role", user.getRole())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return authentication(SecurityConfig.toAuthentication(jwt));
    }

    @BeforeEach
    void anaOwnsProfile30() {
        when(getProfilePort.getById(30L)).thenReturn(ProfileResponse.builder().profileId(30L).userId(7).build());
        when(updateProfilePort.update(eq(30L), any())).thenReturn(ProfileResponse.builder().profileId(30L).build());
        when(getProfileByUserIdPort.getByUserId(7, null)).thenReturn(ProfileResponse.builder().profileId(30L).userId(7).build());
    }

    @Test
    @DisplayName("el perfil público por usuario se lee sin sesión")
    void publicRead() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/user/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId").value(30));
    }

    @Test
    @DisplayName("el dueño y un ADMIN editan; otro usuario recibe 403 sin tocar nada")
    void onlyOwnerOrAdminUpdates() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/30").with(as(LUIS))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PROFILE_FORBIDDEN"));
        verifyNoInteractions(updateProfilePort);

        mockMvc.perform(put("/api/v1/profiles/30").with(as(ANA))
                .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/profiles/30").with(as(ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY)).andExpect(status().isOk());
        verify(updateProfilePort, org.mockito.Mockito.times(2)).update(eq(30L), any());
    }

    @Test
    @DisplayName("nadie crea el perfil de otro usuario; sin sesión, 401")
    void createOnlyOwn() throws Exception {
        mockMvc.perform(post("/api/v1/profiles").with(as(LUIS)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":7,\"fullName\":\"Ana\"}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/profiles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":7,\"fullName\":\"Ana\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/profiles/30").contentType(MediaType.APPLICATION_JSON)
                .content(UPDATE_BODY)).andExpect(status().isUnauthorized());
        verifyNoInteractions(createProfilePort);
    }
}
