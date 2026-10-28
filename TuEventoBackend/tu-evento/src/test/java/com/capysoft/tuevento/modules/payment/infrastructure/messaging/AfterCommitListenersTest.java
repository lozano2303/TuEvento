package com.capysoft.tuevento.modules.payment.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.identitybridge.notification.UserActivatedListener;
import com.capysoft.tuevento.shared.domain.event.identity.UserActivatedEvent;

/**
 * Los listeners con efectos en otro servicio solo se ejecutan si la transacción que publica
 * el evento confirma: una cancelación o una activación que hace rollback no reembolsa ni
 * notifica.
 */
@ActiveProfiles("test")
@SpringBootTest
@DisplayName("Listeners AFTER_COMMIT — reembolsos por cancelación y bienvenida")
class AfterCommitListenersTest {

    @MockitoSpyBean EventCancelledListener eventCancelledListener;
    @MockitoSpyBean UserActivatedListener userActivatedListener;

    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;

    private static EventStatusChangedEvent cancelled() {
        return EventStatusChangedEvent.builder().eventId(404L).oldStatus("PUBLISHED").newStatus("CANCELLED").build();
    }

    @Test
    @DisplayName("si la cancelación hace rollback, no se procesan reembolsos")
    void rollback_noRefunds() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(cancelled());
            status.setRollbackOnly();
        });

        verify(eventCancelledListener, after(500).never()).handleEventStatusChanged(any());
    }

    @Test
    @DisplayName("si la cancelación confirma, se procesan los reembolsos una vez")
    void commit_processesRefunds() {
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(cancelled()));

        verify(eventCancelledListener, timeout(2000).times(1)).handleEventStatusChanged(any());
    }

    @Test
    @DisplayName("si la activación hace rollback, no hay bienvenida")
    void activationRollback_noWelcome() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(UserActivatedEvent.builder().userId(5).alias("ana").build());
            status.setRollbackOnly();
        });

        verify(userActivatedListener, after(500).never()).onUserActivated(any());
    }
}
