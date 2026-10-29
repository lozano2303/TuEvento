package com.capysoft.tuevento.modules.ticket.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * Las sillas de una orden, que son de Catálogo: la saga de compra vista desde Comercio.
 *
 * <p>Hoy el adaptador local hace todo en la transacción del caso de uso. Al separar Catálogo,
 * {@code holdForOrder} pasa a {@code POST /internal/seats/hold} y {@code sell}/{@code release}
 * a los eventos {@code OrderPaid}/{@code OrderReleased}, sin cambiar a quien llama.
 */
public interface CatalogSeatsPort {

    /**
     * Bloquea las sillas para la orden: deben estar reservadas por el usuario, con la reserva
     * vigente y ser del evento. Devuelve el precio de cada una, que la orden copia.
     *
     * @param seatIds sin repetidas y en orden ascendente (así se bloquean, sin interbloqueos)
     * @throws IllegalArgumentException si una silla no existe
     * @throws IllegalStateException    si una silla no cumple las condiciones
     */
    HeldSeats holdForOrder(Long orderId, Long userId, Long eventId, List<Integer> seatIds);

    /** Pago aprobado: las sillas de la orden pasan a vendidas a nombre del comprador. */
    void sell(Long orderId, Long buyerId, List<Integer> seatIds, String reason);

    /** Pago fallido o orden cancelada: las sillas de la orden vuelven a estar disponibles. */
    void release(Long orderId, List<Integer> seatIds, Integer changedBy, String reason);

    /** @param seats en el mismo orden en que se pidieron */
    record HeldSeats(String currency, List<HeldSeat> seats) {

        public BigDecimal total() {
            return seats.stream().map(HeldSeat::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    record HeldSeat(Integer seatId, Integer eventSectionId, BigDecimal price) {
    }
}
