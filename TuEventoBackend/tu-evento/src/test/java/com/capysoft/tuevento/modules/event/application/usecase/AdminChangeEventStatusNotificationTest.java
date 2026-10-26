package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.shared.infrastructure.config.DataInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Regresión: el cambio de estado del admin debe correr en una transacción. Sin ella, el
 * listener AFTER_COMMIT que notifica al organizador (EVENT_PUBLISHED / EVENT_REJECTED) se
 * descartaba en silencio, porque la variante de 3 parámetros era un método default de la
 * interfaz y el proxy de Spring no abría la transacción.
 */
@ActiveProfiles("test")
@SpringBootTest
@DisplayName("AdminChangeEventStatusUseCase — notificación al organizador")
class AdminChangeEventStatusNotificationTest {

    // H2 arranca sin Liquibase: se omite la creación del admin
    @MockitoBean DataInitializer dataInitializer;
    @MockitoBean SendNotificationUseCase sendNotificationUseCase;

    @Autowired AdminChangeEventStatusPort adminChangeEventStatusPort;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("al salir de revisión por la variante que usa el controlador, el organizador recibe su notificación")
    void notifiesTheOrganizerAfterCommit() {
        jdbcTemplate.update("INSERT INTO event (event_id, user_id, site_id, event_name, description, start_date, "
                + "finish_date, status, is_public, available_seats, created_at, updated_at) "
                + "VALUES (901, 11, 1, 'Concierto', 'd', DATE '2099-12-01', DATE '2099-12-02', 'PENDING_REVIEW', "
                + "true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        adminChangeEventStatusPort.execute(901L, ChangeEventStatusRequest.builder()
                .newStatus(EventStatus.REJECTED).reason("Faltan imágenes").build(), 1L);

        ArgumentCaptor<SendNotificationCommand> command = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(sendNotificationUseCase, timeout(5000)).execute(command.capture());
        assertThat(command.getValue().getTypeName()).isEqualTo("EVENT_REJECTED");
        assertThat(command.getValue().getUserIds()).containsExactly(11);
        assertThat(command.getValue().getEntityId()).isEqualTo(901L);
    }
}
