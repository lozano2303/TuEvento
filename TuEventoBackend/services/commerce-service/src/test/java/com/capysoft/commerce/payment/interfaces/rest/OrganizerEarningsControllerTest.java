package com.capysoft.commerce.payment.interfaces.rest;

import com.capysoft.commerce.payment.application.dto.response.OrganizerEarningsResponse;
import com.capysoft.commerce.payment.application.usecase.GetOrganizerEarningsService;
import com.capysoft.common.infrastructure.security.SecurityUser;
import com.capysoft.common.interfaces.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** El panel de ganancias siempre se calcula para el organizador autenticado. */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizerEarningsController")
class OrganizerEarningsControllerTest {

    private static final SecurityUser ORGANIZER = new SecurityUser(42, "org", "ORGANIZER");

    @Mock private GetOrganizerEarningsService getOrganizerEarningsService;
    @InjectMocks private OrganizerEarningsController controller;

    @Test
    @DisplayName("ganancias → con el id del organizador autenticado")
    void getEarnings_usesAuthenticatedOrganizer() {
        OrganizerEarningsResponse earnings = OrganizerEarningsResponse.builder().build();
        when(getOrganizerEarningsService.execute(42L)).thenReturn(earnings);

        ResponseEntity<ApiResponse<OrganizerEarningsResponse>> response = controller.getEarnings(ORGANIZER);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getData()).isSameAs(earnings);
    }
}
