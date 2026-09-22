package com.capysoft.tuevento.modules.payment.application.port.out;

import com.capysoft.tuevento.modules.payment.application.dto.GatewayPayment;
import com.capysoft.tuevento.modules.payment.application.dto.GatewayPaymentEvent;
import com.capysoft.tuevento.modules.payment.application.dto.CreatePaymentCommand;

/**
 * Puerto de salida hacia gateways de pago externos.
 * Contrato genérico - las implementaciones adaptan a cada proveedor específico.
 */
public interface PaymentGatewayPort {
    
    /**
     * Crea un pago en el gateway externo.
     *
     * @param command Comando con datos del pago
     * @return Datos del pago creado en el gateway
     */
    GatewayPayment createPayment(CreatePaymentCommand command);
    
    /**
     * Consulta el estado de un pago en el gateway externo.
     *
     * @param gatewayPaymentId ID del pago en el gateway
     * @return Datos actuales del pago
     */
    GatewayPayment getPayment(String gatewayPaymentId);
    
    /**
     * Cancela un pago en el gateway externo.
     *
     * @param gatewayPaymentId ID del pago en el gateway
     */
    void cancelPayment(String gatewayPaymentId);
    
    /**
     * Procesa y parsea un webhook recibido del gateway.
     *
     * @param payload Body del webhook (JSON)
     * @param signature Firma HMAC del webhook
     * @return Evento parseado del gateway
     */
    GatewayPaymentEvent processWebhook(String payload, String signature);
}
