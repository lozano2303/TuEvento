package com.capysoft.tuevento.modules.ticket.application.port.in;

import java.util.Collection;
import java.util.Set;

/**
 * Puerto de entrada: qué sillas tienen una orden con el pago en curso (PAYMENT_PENDING).
 * Lo consulta el módulo seat para no liberar reservas cuyo pago todavía puede confirmarse.
 */
public interface FindSeatsWithPendingPaymentUseCase {

    /** Devuelve el subconjunto de {@code seatIds} ligado a una orden en PAYMENT_PENDING. */
    Set<Integer> findSeatIdsWithPendingPayment(Collection<Integer> seatIds);
}
