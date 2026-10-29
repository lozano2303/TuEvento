package com.capysoft.tuevento.modules.payment.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/** Sillas de los eventos de un organizador, para su ganancia potencial. Las aporta Catálogo. */
public interface OrganizerSeatsPort {

    /** Una fila por evento no cancelado del organizador que tenga sillas. */
    List<EventSeats> summarizeByOrganizer(Long organizerId);

    /**
     * @param potentialRevenue suma del precio de su sección de cada silla que no es de cortesía
     * @param courtesySeats    sillas de cortesía (no se venden)
     * @param totalSeats       todas las sillas del evento
     */
    record EventSeats(Long eventId, String eventName, BigDecimal potentialRevenue,
                      Long courtesySeats, Long totalSeats) {
    }
}
