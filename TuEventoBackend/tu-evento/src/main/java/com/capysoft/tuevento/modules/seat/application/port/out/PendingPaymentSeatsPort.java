package com.capysoft.tuevento.modules.seat.application.port.out;

import java.util.Collection;
import java.util.Set;

/**
 * Puerto de salida hacia Comercio: sillas cuya orden tiene el pago en curso.
 * El scheduler de expiración no debe liberarlas, porque un webhook tardío las vendería.
 */
public interface PendingPaymentSeatsPort {

    /** Devuelve el subconjunto de {@code seatIds} ligado a una orden en PAYMENT_PENDING, en una sola consulta. */
    Set<Integer> findSeatIdsWithPendingPayment(Collection<Integer> seatIds);
}
