package com.capysoft.tuevento.modules.seat.interfaces.rest;

import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenVerifier;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Crear, cambiar de estado y borrar sillas: solo el organizador (su id llega al caso de uso,
 * que comprueba que el evento sea suyo); ADMIN solo consulta, igual que en las secciones.
 */
@WebMvcTest(SeatController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
@DisplayName("SeatController security")
class SeatControllerSecurityTest {

    private static final SecurityUser ORGANIZER = new SecurityUser(42, "org", "ORGANIZER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");
    private static final String CREATE_BODY =
            "{\"seatBlockId\":1,\"eventSectionId\":70,\"code\":\"Z9\",\"row\":9,\"position\":9,\"type\":\"REGULAR\"}";
    private static final String COURTESY_BODY = "{\"newStatus\":\"COURTESY\",\"reason\":\"invitado\"}";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AccessTokenVerifier accessTokenVerifier;
    @MockitoBean private SeatUseCase seatUseCase;

    @Test
    @DisplayName("sin sesión no se puede cambiar una silla")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/seats/10/status").contentType(MediaType.APPLICATION_JSON).content(COURTESY_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(seatUseCase);
    }

    @Test
    @DisplayName("ADMIN solo consulta: crear, cambiar de estado y borrar responden 403")
    void adminIsReadOnly() throws Exception {
        mockMvc.perform(post("/api/v1/seats").with(user(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/seats/10/status").with(user(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(COURTESY_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/seats/10").with(user(ADMIN)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(seatUseCase);
    }

    @Test
    @DisplayName("el organizador marca una cortesía con su propio id (no el cambio interno sin validación)")
    void organizerUsesTheOwnershipCheckedPath() throws Exception {
        when(seatUseCase.updateSeatStatusByOrganizer(eq(10), any(), eq(42L)))
                .thenReturn(SeatResponse.builder().seatId(10).status(SeatStatus.COURTESY).build());

        mockMvc.perform(patch("/api/v1/seats/10/status").with(user(ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON).content(COURTESY_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COURTESY"));
    }

    @Test
    @DisplayName("el organizador crea y borra con su propio id")
    void organizerCreatesAndDeletesWithOwnId() throws Exception {
        when(seatUseCase.createSeat(any(), eq(42L))).thenReturn(SeatResponse.builder().seatId(11).build());

        mockMvc.perform(post("/api/v1/seats").with(user(ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seatId").value(11));
        mockMvc.perform(delete("/api/v1/seats/11").with(user(ORGANIZER)))
                .andExpect(status().isOk());

        verify(seatUseCase).deleteSeat(11, 42L);
    }
}
