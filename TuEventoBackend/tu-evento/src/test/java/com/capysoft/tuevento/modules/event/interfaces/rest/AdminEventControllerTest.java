package com.capysoft.tuevento.modules.event.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.application.port.in.GetAdminEventsPort;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;

@SpringJUnitConfig({AdminEventController.class, AdminEventControllerTest.MethodSecurityConfig.class})
@DisplayName("AdminEventController (Catálogo)")
class AdminEventControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @MockitoBean GetAdminEventsPort getAdminEventsPort;
    @MockitoBean AdminChangeEventStatusPort adminChangeEventStatusPort;
    @Autowired AdminEventController controller;

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("lista y cambia el estado de eventos con el admin como autor")
    void adminDelegates() {
        controller.getEvents(EventStatus.PENDING_REVIEW);
        ChangeEventStatusRequest request = ChangeEventStatusRequest.builder().newStatus(EventStatus.PUBLISHED).build();
        controller.changeEventStatus(5L, request, new SecurityUser(1, "admin", "ADMIN"));

        verify(getAdminEventsPort).getEvents(EventStatus.PENDING_REVIEW);
        verify(adminChangeEventStatusPort).execute(eq(5L), eq(request), eq(1L));
    }

    @Test
    @WithMockUser(authorities = "ORGANIZER")
    @DisplayName("un organizador no puede usar el admin de eventos")
    void organizerIsDenied() {
        assertThatThrownBy(() -> controller.getEvents(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.changeEventStatus(5L, null, new SecurityUser(2, "o", "ORGANIZER")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(getAdminEventsPort, adminChangeEventStatusPort);
    }
}
