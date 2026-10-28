package com.capysoft.tuevento.modules.ticket.infrastructure.messaging;

import com.capysoft.tuevento.modules.ticket.domain.event.TicketsValidatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Avisa al asistente por su canal personal {@code /topic/users/{userId}/tickets} que el
 * organizador validó su QR. Solo después del commit: si el escaneo se revierte, no se avisa.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketValidatedWebSocketListener {

    static final String TYPE = "QR_VALIDATED";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketsValidated(TicketsValidatedEvent event) {
        String topic = "/topic/users/" + event.userId() + "/tickets";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", TYPE);
        payload.put("scope", event.scope());
        payload.put("targetId", event.targetId());
        payload.put("eventId", event.eventId());
        payload.put("ticketIds", event.ticketIds());

        try {
            messagingTemplate.convertAndSend(topic, payload);
        } catch (RuntimeException e) {
            // El check-in ya quedó guardado; la app lo detecta igual consultando el ticket
            log.warn("[WebSocket] No se pudo avisar la validación del QR a {}: {}", topic, e.getMessage());
        }
    }
}
