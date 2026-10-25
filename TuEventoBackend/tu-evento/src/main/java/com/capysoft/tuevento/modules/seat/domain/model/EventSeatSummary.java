package com.capysoft.tuevento.modules.seat.domain.model;

import java.math.BigDecimal;

/**
 * Resumen de las sillas de un evento para calcular su ganancia potencial.
 *
 * @param potentialRevenue suma del precio de su sección de cada silla que no es de cortesía
 * @param courtesySeats    sillas de cortesía (no se venden)
 * @param totalSeats       todas las sillas del evento
 */
public record EventSeatSummary(Long eventId, String eventName, BigDecimal potentialRevenue,
                               Long courtesySeats, Long totalSeats) {
}
