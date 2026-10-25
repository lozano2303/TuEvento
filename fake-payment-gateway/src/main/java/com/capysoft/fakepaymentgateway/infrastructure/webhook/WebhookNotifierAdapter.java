package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import org.springframework.stereotype.Component;

import com.capysoft.fakepaymentgateway.application.port.out.WebhookNotifierPort;
import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;

/**
 * Adaptador que implementa el envío de webhooks con firma HMAC y reintentos.
 *
 * El primer intento es síncrono para que participe en la transacción: si falla
 * el save() del pago, el webhook nunca se envía. Los reintentos se delegan a
 * {@link WebhookRetryScheduler}, que corre en otro hilo y no bloquea la petición.
 */
@Component
public class WebhookNotifierAdapter implements WebhookNotifierPort {
    private final WebhookDeliveryClient deliveryClient;
    private final WebhookRetryScheduler retryScheduler;

    public WebhookNotifierAdapter(
        WebhookDeliveryClient deliveryClient,
        WebhookRetryScheduler retryScheduler
    ) {
        this.deliveryClient = deliveryClient;
        this.retryScheduler = retryScheduler;
    }

    @Override
    public void notify(PaymentStatusChanged event) {
        if (!deliveryClient.deliver(event, 1)) {
            retryScheduler.scheduleRetries(event);
        }
    }
}
