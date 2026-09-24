package com.capysoft.tuevento.modules.ticket.application.port.in;

/**
 * Puerto de entrada para confirmar el reembolso de una orden.
 * Será invocado por el módulo payment cuando llegue el webhook payment.refunded.
 *
 * Transiciona Order: PAID → REFUNDED y sus Tickets a REFUNDED.
 * Las sillas NO se liberan — ya están vendidas/usadas o el evento pasó/fue cancelado.
 */
public interface ConfirmRefundUseCase {

    /**
     * Transiciona la orden y sus tickets a estado REFUNDED.
     *
     * @param orderId ID de la orden a reembolsar
     */
    void confirmRefund(Long orderId);
}
