package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.modules.section.application.dto.response.EventSectionResponse;
import com.capysoft.tuevento.modules.section.application.port.in.EventSectionUseCase;
import com.capysoft.tuevento.modules.security.application.port.out.CurrentRolePort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityConfig;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las secciones de un evento solo las modifica su organizador: ADMIN solo consulta y
 * el id del usuario autenticado llega al caso de uso para comprobar la propiedad.
 */
@WebMvcTest(EventSectionController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
@DisplayName("EventSectionController security")
class EventSectionControllerSecurityTest {

    private static final SecurityUser ORGANIZER = new SecurityUser(42, "org", "ORGANIZER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");
    private static final String CREATE_BODY =
            "{\"eventId\":5,\"sectionTypeId\":2,\"capacity\":50,\"price\":20000}";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TokenGeneratorPort tokenGenerator;
    @MockitoBean private CurrentRolePort currentRolePort;
    @MockitoBean private EventSectionUseCase eventSectionUseCase;

    @Test
    @DisplayName("GET por evento sigue siendo público")
    void listIsPublic() throws Exception {
        when(eventSectionUseCase.getSectionsByEvent(5, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/event-sections/event/5")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("sin sesión no se puede crear")
    void anonymousCannotCreate() throws Exception {
        mockMvc.perform(post("/api/v1/event-sections").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(eventSectionUseCase);
    }

    @Test
    @DisplayName("ADMIN solo consulta: crear, modificar y borrar responden 403")
    void adminIsReadOnly() throws Exception {
        mockMvc.perform(post("/api/v1/event-sections").with(user(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/event-sections/30").with(user(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/event-sections/30").with(user(ADMIN)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(eventSectionUseCase);
    }

    @Test
    @DisplayName("el organizador crea y su id llega al caso de uso")
    void organizerCreatesWithOwnId() throws Exception {
        when(eventSectionUseCase.createEventSection(any(), eq(42L)))
                .thenReturn(EventSectionResponse.builder().eventSectionId(30).eventId(5).build());

        mockMvc.perform(post("/api/v1/event-sections").with(user(ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.eventSectionId").value(30));
    }

    @Test
    @DisplayName("el organizador modifica y borra con su propio id")
    void organizerUpdatesAndDeletesWithOwnId() throws Exception {
        when(eventSectionUseCase.updateEventSection(eq(30), any(), eq(42L)))
                .thenReturn(EventSectionResponse.builder().eventSectionId(30).build());

        mockMvc.perform(put("/api/v1/event-sections/30").with(user(ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/event-sections/30").with(user(ORGANIZER)))
                .andExpect(status().isOk());

        verify(eventSectionUseCase).deleteEventSection(30, 42L);
    }

    @Test
    @DisplayName("un evento ajeno responde 403 con EVENT_ACCESS_DENIED")
    void foreignEventKeepsItsCode() throws Exception {
        doThrow(new ForbiddenException("EVENT_ACCESS_DENIED", "No eres el organizador de este evento"))
                .when(eventSectionUseCase).deleteEventSection(30, 42L);

        mockMvc.perform(delete("/api/v1/event-sections/30").with(user(ORGANIZER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EVENT_ACCESS_DENIED"));
    }
}
