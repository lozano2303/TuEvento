package com.capysoft.tuevento.modules.profile.interfaces.rest;

import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.in.CreateProfilePort;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfileByUserIdPort;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfilePort;
import com.capysoft.tuevento.modules.profile.application.port.in.UpdateProfilePort;
import com.capysoft.tuevento.modules.security.application.port.out.CurrentRolePort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityConfig;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión: con cualquier sesión se podía modificar el perfil de otra persona cambiando el ID de
 * la URL, o crear el de otro usuario cambiando el userId del cuerpo. Solo el dueño o un ADMIN.
 */
@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
@DisplayName("ProfileController — solo el dueño o un ADMIN modifica un perfil")
class ProfileControllerSecurityTest {

    private static final SecurityUser ANA = new SecurityUser(7, "ana", "USER");
    private static final SecurityUser LUIS = new SecurityUser(8, "luis", "USER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");
    private static final String UPDATE_BODY = "{\"fullName\":\"Nombre Cambiado\"}";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TokenGeneratorPort tokenGenerator;
    @MockitoBean private CurrentRolePort currentRolePort;
    @MockitoBean private CreateProfilePort createProfilePort;
    @MockitoBean private UpdateProfilePort updateProfilePort;
    @MockitoBean private GetProfilePort getProfilePort;
    @MockitoBean private GetProfileByUserIdPort getProfileByUserIdPort;

    @BeforeEach
    void anaOwnsProfile30() {
        when(getProfilePort.getById(30L)).thenReturn(ProfileResponse.builder().profileId(30L).userId(7).build());
        when(updateProfilePort.update(eq(30L), any())).thenReturn(ProfileResponse.builder().profileId(30L).userId(7).build());
        when(createProfilePort.create(any())).thenReturn(ProfileResponse.builder().profileId(31L).userId(7).build());
    }

    @Test
    @DisplayName("el dueño edita su perfil")
    void ownerUpdates() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/30").with(user(ANA))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk());
        verify(updateProfilePort).update(eq(30L), any());
    }

    @Test
    @DisplayName("otro usuario no puede editar ni crear un perfil ajeno: 403 PROFILE_FORBIDDEN")
    void otherUserIsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/30").with(user(LUIS))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_FORBIDDEN"));
        mockMvc.perform(post("/api/v1/profiles").with(user(LUIS))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":7,\"fullName\":\"Ana\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(updateProfilePort, createProfilePort);
    }

    @Test
    @DisplayName("un ADMIN puede editar cualquier perfil; cada usuario crea el suyo")
    void adminAndOwnCreation() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/30").with(user(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/profiles").with(user(ANA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":7,\"fullName\":\"Ana\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("sin sesión no se edita ni se crea nada")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(put("/api/v1/profiles/30").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                        .isIn(401, 403));
        mockMvc.perform(post("/api/v1/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":7,\"fullName\":\"Ana\"}"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                        .isIn(401, 403));
        verifyNoInteractions(updateProfilePort, createProfilePort);
    }
}
