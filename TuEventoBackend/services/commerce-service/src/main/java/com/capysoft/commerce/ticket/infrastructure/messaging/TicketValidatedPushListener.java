package com.capysoft.commerce.ticket.infrastructure.messaging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.commerce.ticket.domain.event.TicketsValidatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Avisa al asistente por su canal personal {@code /topic/users/{userId}/tickets} que el
 * organizador validó su QR. Solo después del commit: si el escaneo se revierte, no se avisa.
 * El /ws no vive en este servicio: el aviso sale por Kafka, como los de notification-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketValidatedPushListener {

    /** Comercio → quien emita el /ws. */
    static final String PUSH_TOPIC = "tuevento.comercio.push";
    static final String TYPE = "QR_VALIDATED";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketsValidated(TicketsValidatedEvent event) {
        String destination = "/topic/users/" + event.userId() + "/tickets";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", TYPE);
        body.put("scope", event.scope());
        body.put("targetId", event.targetId());
        body.put("eventId", event.eventId());
        body.put("ticketIds", event.ticketIds());
        try {
            kafkaTemplate.send(PUSH_TOPIC, String.valueOf(event.userId()),
                    objectMapper.writeValueAsString(Map.of("destination", destination, "body", body)));
        } catch (Exception e) {
            // El check-in ya quedó guardado; la app lo detecta igual consultando el ticket
            log.warn("Could not publish QR validation push for {}: {}", destination, e.getMessage());
        }
    }
}
