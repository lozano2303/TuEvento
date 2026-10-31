package com.capysoft.commerce.ticket.domain.event;

import java.util.List;

/**
 * El organizador validó con un QR tickets de un asistente. Se publica uno por dueño de los
 * tickets, para avisarle en vivo y que su pantalla deje de mostrar el QR.
 *
 * @param userId    dueño de los tickets validados
 * @param scope     alcance del QR escaneado: TICKET u ORDER
 * @param targetId  ticketId u orderId del QR, según el alcance
 * @param eventId   evento al que pertenecen los tickets
 * @param ticketIds tickets que quedaron USED con este escaneo
 */
public record TicketsValidatedEvent(Long userId, String scope, Long targetId, Long eventId, List<Long> ticketIds) {

    public TicketsValidatedEvent {
        ticketIds = List.copyOf(ticketIds);
    }
}
