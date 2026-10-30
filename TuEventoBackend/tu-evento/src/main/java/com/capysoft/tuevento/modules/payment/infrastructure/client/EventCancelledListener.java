package com.capysoft.tuevento.modules.payment.infrastructure.client;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.payment.application.port.in.RefundCancelledEventUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Único punto de Comercio que escucha los cambios de estado de un evento de Catálogo. Hoy es un
 * listener en memoria; después será un consumidor Kafka de {@code EventStatusChanged}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCancelledListener {

    private static final String CANCELLED = "CANCELLED";

    private final RefundCancelledEventUseCase refundCancelledEvent;

    /**
     * Usa {@code @TransactionalEventListener(AFTER_COMMIT)} para que los reembolsos
     * solo se procesen si la cancelación del evento se confirmó en base de datos: con
     * {@code @EventListener} se dispararían aunque la cancelación hiciera rollback.
     * {@code @Async} los saca del hilo de la petición, porque llaman a la pasarela.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEventStatusChanged(EventStatusChangedEvent event) {
        if (!CANCELLED.equals(event.getNewStatus())) {
            return;
        }
        try {
            refundCancelledEvent.execute(event.getEventId());
        } catch (Exception e) {
            log.error("Unexpected error processing event cancellation refunds: eventId={}", event.getEventId(), e);
        }
    }
}
