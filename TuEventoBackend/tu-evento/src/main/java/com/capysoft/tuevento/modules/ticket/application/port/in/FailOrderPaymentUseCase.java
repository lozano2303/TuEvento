package com.capysoft.tuevento.modules.ticket.application.port.in;

/**
 * Puerto de entrada para marcar una orden como pago fallido.
 * Será invocado por el módulo payment cuando el pago falle.
 */
public interface FailOrderPaymentUseCase {
    /**
     * Marca una orden como pago fallido y libera las sillas si corresponde.
     *
     * @param orderId ID de la orden
     * @param reason Razón del fallo
     */
    void failOrderPayment(Long orderId, String reason);
}
