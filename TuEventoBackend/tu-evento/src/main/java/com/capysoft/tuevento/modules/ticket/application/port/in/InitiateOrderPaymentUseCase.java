package com.capysoft.tuevento.modules.ticket.application.port.in;

/**
 * Puerto de entrada para iniciar el proceso de pago de una orden.
 * Transiciona Order de DRAFT a PAYMENT_PENDING.
 * Será invocado por el módulo payment antes de crear el pago en el gateway.
 */
public interface InitiateOrderPaymentUseCase {
    /**
     * Inicia el proceso de pago de una orden, transicionándola a PAYMENT_PENDING.
     * Valida que la orden esté en DRAFT antes de transicionar.
     *
     * @param orderId ID de la orden
     * @throws com.capysoft.tuevento.modules.ticket.domain.model.OrderNotFoundException si no existe
     * @throws com.capysoft.tuevento.modules.ticket.domain.model.InvalidOrderStatusTransitionException si no está en DRAFT
     */
    void initiateOrderPayment(Long orderId);
}
