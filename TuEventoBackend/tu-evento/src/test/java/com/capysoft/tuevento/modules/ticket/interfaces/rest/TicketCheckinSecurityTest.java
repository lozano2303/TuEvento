package com.capysoft.tuevento.modules.ticket.interfaces.rest;

import com.capysoft.tuevento.modules.ticket.application.usecase.CheckinTicketUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GenerateTicketQrUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetTicketUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetUserTicketsUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.ScanQrUseCaseImpl;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * El check-in manual exige rol ORGANIZER y usa el usuario autenticado como validador.
 */
@SpringJUnitConfig({TicketController.class, TicketCheckinSecurityTest.MethodSecurityConfig.class})
@DisplayName("TicketController check-in security")
class TicketCheckinSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @MockitoBean private GetTicketUseCaseImpl getTicketUseCase;
    @MockitoBean private GetUserTicketsUseCaseImpl getUserTicketsUseCase;
    @MockitoBean private CheckinTicketUseCaseImpl checkinTicketUseCase;
    @MockitoBean private GenerateTicketQrUseCaseImpl generateTicketQrUseCase;
    @MockitoBean private ScanQrUseCaseImpl scanQrUseCase;

    @Autowired
    private TicketController controller;

    private static final SecurityUser ORGANIZER = new SecurityUser(30, "org", "ORGANIZER");
    private static final SecurityUser USER = new SecurityUser(7, "user", "USER");

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("USER → AccessDeniedException, the use case is never called")
    void checkin_user_isDenied() {
        assertThatThrownBy(() -> controller.checkinTicket(200L, USER))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(checkinTicketUseCase);
    }

    @Test
    @WithMockUser(authorities = "ORGANIZER")
    @DisplayName("ORGANIZER → delegates with the authenticated organizer as validator")
    void checkin_organizer_usesAuthenticatedValidator() {
        controller.checkinTicket(200L, ORGANIZER);
        verify(checkinTicketUseCase).execute(200L, 30L);
    }
}
