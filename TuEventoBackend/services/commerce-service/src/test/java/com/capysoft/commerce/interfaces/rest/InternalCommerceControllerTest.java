package com.capysoft.commerce.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.capysoft.commerce.infrastructure.security.SecurityConfig;
import com.capysoft.commerce.interfaces.advice.GlobalExceptionHandler;
import com.capysoft.commerce.payment.application.port.in.GetCommerceAdminStatsPort;
import com.capysoft.commerce.ticket.application.port.in.CountTicketsSoldUseCase;
import com.capysoft.commerce.ticket.application.port.in.FindSeatsWithPendingPaymentUseCase;
import com.capysoft.common.infrastructure.security.InternalApiKeyFilter;

/** Commerce internal API behind the real {@link SecurityConfig}: only with the service-to-service key. */
@WebMvcTest(InternalCommerceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "app.internal.api-key=test-key")
@DisplayName("InternalCommerceController")
class InternalCommerceControllerTest {

    private static final String KEY = InternalApiKeyFilter.HEADER;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private FindSeatsWithPendingPaymentUseCase pendingPaymentSeats;
    @MockitoBean private GetCommerceAdminStatsPort adminStats;
    @MockitoBean private CountTicketsSoldUseCase ticketsSold;

    @Test
    @DisplayName("without the key it answers 401 and never reaches the use case")
    void rejectsWithoutKey() throws Exception {
        mockMvc.perform(post("/internal/events/tickets-sold")
                        .contentType(MediaType.APPLICATION_JSON).content("[1]"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INTERNAL_AUTH_REQUIRED"));

        verifyNoInteractions(ticketsSold);
    }

    @Test
    @DisplayName("returns the tickets sold of each requested event")
    void ticketsSoldPerEvent() throws Exception {
        when(ticketsSold.countSoldByEventIds(any())).thenReturn(Map.of(1L, 3L, 2L, 0L));

        mockMvc.perform(post("/internal/events/tickets-sold").header(KEY, "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['1']").value(3))
                .andExpect(jsonPath("$.data['2']").value(0));

        verify(ticketsSold).countSoldByEventIds(List.of(1L, 2L));
    }
}
