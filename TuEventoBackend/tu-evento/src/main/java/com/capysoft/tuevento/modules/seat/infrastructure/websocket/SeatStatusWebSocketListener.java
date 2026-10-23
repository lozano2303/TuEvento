package com.capysoft.tuevento.modules.seat.infrastructure.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.seat.domain.event.SeatStatusChangedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener que escucha eventos de cambio de estado de sillas y los transmite
 * a los clientes conectados vía WebSocket.
 * 
 * Los clientes deben suscribirse al topic: /topic/events/{eventId}/seats
 * 
 * Casos de uso:
 * - Usuario A reserva una silla → todos los usuarios viendo ese evento reciben
 *   la actualización en tiempo real.
 * - Una reserva expira automáticamente → el scheduler libera la silla y se
 *   notifica a todos los clientes.
 * - Usuario B libera su reserva manualmente → se propaga el cambio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatStatusWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onSeatStatusChanged(SeatStatusChangedEvent event) {
        String topic = "/topic/events/" + event.getEventId() + "/seats";
        
        log.debug("[WebSocket] Broadcasting seat status change to {}: seatId={}, {} -> {}", 
                topic, event.getSeatId(), event.getOldStatus(), event.getNewStatus());

        messagingTemplate.convertAndSend(topic, event);
    }
}
