package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;

/**
 * Ejecuta los reintentos de webhook en un hilo del pool asíncrono.
 *
 * Es un bean separado de {@link WebhookNotifierAdapter} a propósito: @Async solo
 * se aplica cuando el método público se invoca a través del proxy de Spring,
 * nunca en una llamada interna de la misma clase.
 */
@Component
public class WebhookRetryScheduler {
    private static final Logger log = LoggerFactory.getLogger(WebhookRetryScheduler.class);

    private final WebhookDeliveryClient deliveryClient;
    private final long[] backoffDelaysMs;

    public WebhookRetryScheduler(
        WebhookDeliveryClient deliveryClient,
        @Value("${webhook.retry.backoff-ms:2000,10000}") long[] backoffDelaysMs
    ) {
        this.deliveryClient = deliveryClient;
        this.backoffDelaysMs = backoffDelaysMs.clone();
    }

    /** Número total de intentos: el primero síncrono más un reintento por cada espera configurada. */
    public int maxAttempts() {
        return backoffDelaysMs.length + 1;
    }

    /**
     * Reintenta la entrega desde el intento 2 hasta {@link #maxAttempts()},
     * esperando el backoff configurado antes de cada uno.
     */
    @Async
    public void scheduleRetries(PaymentStatusChanged event) {
        for (int attempt = 2; attempt <= maxAttempts(); attempt++) {
            try {
                Thread.sleep(backoffDelaysMs[attempt - 2]);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Webhook retry interrupted: eventId={}", event.eventId());
                return;
            }

            if (deliveryClient.deliver(event, attempt)) {
                return;
            }
        }

        log.error("Webhook delivery failed after {} attempts: eventId={}", maxAttempts(), event.eventId());
    }
}
