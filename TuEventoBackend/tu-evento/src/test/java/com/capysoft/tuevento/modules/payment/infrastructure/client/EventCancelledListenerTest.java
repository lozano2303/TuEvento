package com.capysoft.tuevento.modules.payment.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.payment.application.port.in.RefundCancelledEventUseCase;

@DisplayName("EventCancelledListener")
class EventCancelledListenerTest {

    private final RefundCancelledEventUseCase refunds = mock(RefundCancelledEventUseCase.class);
    private final EventCancelledListener listener = new EventCancelledListener(refunds);

    private static EventStatusChangedEvent changedTo(String status) {
        return EventStatusChangedEvent.builder().eventId(9L).oldStatus("PUBLISHED").newStatus(status).build();
    }

    @Test
    @DisplayName("solo reembolsa cuando el evento pasa a CANCELLED")
    void onlyCancelledTriggersRefunds() {
        listener.handleEventStatusChanged(changedTo("COMPLETED"));
        verifyNoInteractions(refunds);

        listener.handleEventStatusChanged(changedTo("CANCELLED"));
        verify(refunds).execute(9L);
    }

    @Test
    @DisplayName("un error inesperado se registra y no sale del hilo asíncrono")
    void unexpectedErrorIsContained() {
        doThrow(new IllegalStateException("db down")).when(refunds).execute(9L);

        assertThatCode(() -> listener.handleEventStatusChanged(changedTo("CANCELLED"))).doesNotThrowAnyException();
    }
}
