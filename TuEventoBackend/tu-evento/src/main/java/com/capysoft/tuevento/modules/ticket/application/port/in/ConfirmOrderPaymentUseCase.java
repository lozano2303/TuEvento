package com.capysoft.tuevento.modules.ticket.application.port.in;

/**
 * Puerto de entrada para confirmar el pago de una orden.
 * Será invocado por el módulo payment una vez confirmado el pago exitoso.
 */
public interface ConfirmOrderPaymentUseCase {
    /**
     * Confirma el pago de una orden y transiciona Order y Tickets a PAID.
     *
     * @param orderId ID de la orden
     * @param providerPaymentId ID del pago del proveedor (ej. "fake_xxx" del gateway)
     */
    void confirmOrderPayment(Long orderId, String providerPaymentId);
}
