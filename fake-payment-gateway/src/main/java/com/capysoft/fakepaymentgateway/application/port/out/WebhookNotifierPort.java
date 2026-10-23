package com.capysoft.fakepaymentgateway.application.port.out;

import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;

/**
 * Puerto de salida para notificaciones webhook.
 * La infraestructura implementa el envío HTTP con firma y reintentos.
 */
public interface WebhookNotifierPort {
    /**
     * Notifica un cambio de estado de pago al sistema externo.
     * 
     * @param event Evento con los datos del cambio de estado
     */
    void notify(PaymentStatusChanged event);
}
