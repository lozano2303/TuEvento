package com.capysoft.notification.interfaces.rest;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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

import com.capysoft.common.domain.exception.NotFoundException;
import com.capysoft.notification.application.usecase.InAppNotificationQueryUseCase;
import com.capysoft.notification.infrastructure.security.SecurityConfig;
import com.capysoft.notification.interfaces.advice.GlobalExceptionHandler;

/**
 * La bandeja exige un JWT de Identidad y siempre usa el usuario del token: nadie puede leer ni
 * marcar notificaciones de otro.
 */
@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("NotificationController security")
class NotificationControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    // Sin servidor JWKS en el test: la autenticación se inyecta con el mismo conversor que en producción
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private InAppNotificationQueryUseCase useCase;

    private static RequestPostProcessor user(int userId) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("user" + userId)
                .claim("userId", userId).claim("role", "USER")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return authentication(SecurityConfig.toAuthentication(jwt));
    }

    @Test
    @DisplayName("sin token, los 4 endpoints responden 401 sin llegar al caso de uso")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications/me/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/notifications/5/read")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/notifications/me/read-all")).andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("el contador y marcar todas usan el usuario del token")
    void usesTheTokenUser() throws Exception {
        when(useCase.unreadCount(7)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/me/unread-count").with(user(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
        mockMvc.perform(patch("/api/v1/notifications/me/read-all").with(user(7))).andExpect(status().isOk());

        verify(useCase).markAllRead(7);
    }

    @Test
    @DisplayName("marcar como leída una notificación ajena da 404 con el formato ApiResponse")
    void otherUsersNotification_isNotFound() throws Exception {
        doThrow(new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found"))
                .when(useCase).markRead(7, 5L);

        mockMvc.perform(patch("/api/v1/notifications/5/read").with(user(7)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
