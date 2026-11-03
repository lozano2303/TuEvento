package com.capysoft.commerce.ticket.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Las sillas de una orden, que son de Catálogo: la saga de compra vista desde Comercio.
 *
 * <p>{@code holdForOrder} es síncrono ({@code POST /internal/seats/hold}). {@code sell} y
 * {@code release} se guardan en el outbox dentro de la transacción de quien llama
 * ({@code OrderPaid} y {@code OrderReleased}): Catálogo los aplica después, así que el webhook de la
 * pasarela nunca depende de que Catálogo esté arriba.
 */
public interface CatalogSeatsPort {

    /**
     * Bloquea las sillas para la orden: deben estar reservadas por el usuario, con la reserva
     * vigente y ser del evento. Devuelve el precio de cada una, que la orden copia.
     *
     * @param seatIds sin repetidas y en orden ascendente (así se bloquean, sin interbloqueos)
     * @throws IllegalStateException si una silla no cumple las condiciones
     */
    HeldSeats holdForOrder(Long orderId, Long userId, Long eventId, List<Integer> seatIds);

    /** Pago aprobado: las sillas de la orden pasan a vendidas a nombre del comprador. */
    void sell(Long orderId, Long buyerId, Long eventId, List<Integer> seatIds, String providerPaymentId);

    /**
     * Pago fallido o orden cancelada: las sillas de la orden vuelven a estar disponibles.
     *
     * @param detail texto libre para el historial de la silla
     */
    void release(Long orderId, Long buyerId, List<Integer> seatIds, Integer changedBy, ReleaseReason reason, String detail);

    enum ReleaseReason {
        FAILED, CANCELLED, EXPIRED
    }

    /** @param seats en el mismo orden en que se pidieron */
    record HeldSeats(String currency, List<HeldSeat> seats) {

        public BigDecimal total() {
            return seats.stream().map(HeldSeat::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    record HeldSeat(Integer seatId, Integer eventSectionId, BigDecimal price) {
    }
}
