package com.capysoft.tuevento.modules.ticket.application.port.in;

import com.capysoft.tuevento.modules.ticket.application.dto.response.RefundableTicketsSummary;

/**
 * Puerto de entrada para confirmar el reembolso de una orden.
 * Lo invoca el módulo payment (webhook payment.refunded, reembolsos locales y
 * cancelación de eventos).
 *
 * Solo se reembolsan los tickets PAID: los USED nunca vuelven a REFUNDED.
 * Las sillas NO se liberan — ya están vendidas/usadas o el evento pasó/fue cancelado.
 */
public interface ConfirmRefundUseCase {

    /**
     * Calcula qué tickets de la orden son reembolsables (PAID) y cuánto suman.
     *
     * @param orderId ID de la orden
     */
    RefundableTicketsSummary getRefundableTickets(Long orderId);

    /**
     * Pasa a REFUNDED los tickets PAID de la orden. La orden pasa a REFUNDED solo si
     * todos sus tickets se reembolsaron; si alguno se usó, sigue en PAID con cada
     * ticket en su estado real (USED o REFUNDED).
     *
     * @param orderId ID de la orden a reembolsar
     */
    void confirmRefund(Long orderId);
}
