package com.capysoft.tuevento.modules.payment.domain.model;

import com.capysoft.tuevento.shared.domain.exception.BusinessException;

/**
 * Excepción lanzada cuando el gateway de pagos externo falla (timeout, caído, error HTTP).
 * Se usa para propagar errores técnicos del gateway al cliente de manera clara.
 */
public class PaymentGatewayException extends BusinessException {

    public PaymentGatewayException(String message) {
        super("PAYMENT_GATEWAY_ERROR", message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super("PAYMENT_GATEWAY_ERROR", message + " - " + cause.getMessage());
    }
}
