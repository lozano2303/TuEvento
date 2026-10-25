package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.domain.model.Money;
import com.capysoft.fakepaymentgateway.domain.model.PaymentStatus;

/**
 * Cubre L-01: los reintentos no deben ejecutarse en el hilo de la petición.
 */
class WebhookRetryTest {

    private static final PaymentStatusChanged EVENT = new PaymentStatusChanged(
        "evt-1", "fake_1", PaymentStatus.APPROVED, new Money(BigDecimal.TEN, "COP"), Instant.now());

    @Test
    void notifierNoReintentaSiElPrimerIntentoTieneExito() {
        WebhookDeliveryClient client = mock(WebhookDeliveryClient.class);
        WebhookRetryScheduler scheduler = mock(WebhookRetryScheduler.class);
        when(client.deliver(EVENT, 1)).thenReturn(true);

        new WebhookNotifierAdapter(client, scheduler).notify(EVENT);

        verify(scheduler, never()).scheduleRetries(any());
    }

    @Test
    void notifierDelegaLosReintentosSiElPrimerIntentoFalla() {
        WebhookDeliveryClient client = mock(WebhookDeliveryClient.class);
        WebhookRetryScheduler scheduler = mock(WebhookRetryScheduler.class);
        when(client.deliver(EVENT, 1)).thenReturn(false);

        new WebhookNotifierAdapter(client, scheduler).notify(EVENT);

        verify(scheduler).scheduleRetries(EVENT);
    }

    @Test
    void schedulerHaceLosIntentosRestantesYSeDetieneAlTenerExito() {
        WebhookDeliveryClient client = mock(WebhookDeliveryClient.class);
        when(client.deliver(EVENT, 2)).thenReturn(true);
        WebhookRetryScheduler scheduler = new WebhookRetryScheduler(client, new long[] {0, 0});

        scheduler.scheduleRetries(EVENT);

        verify(client).deliver(EVENT, 2);
        verify(client, never()).deliver(EVENT, 3);
    }

    @Test
    void elBackoffSeLeeDeLaConfiguracionSeparadoPorComas() {
        new ApplicationContextRunner()
            .withBean(WebhookDeliveryClient.class, () -> mock(WebhookDeliveryClient.class))
            .withUserConfiguration(WebhookRetryScheduler.class)
            .withPropertyValues("webhook.retry.backoff-ms=100,200,300")
            .run(ctx -> assertThat(ctx.getBean(WebhookRetryScheduler.class).maxAttempts()).isEqualTo(4));

        new ApplicationContextRunner()
            .withBean(WebhookDeliveryClient.class, () -> mock(WebhookDeliveryClient.class))
            .withUserConfiguration(WebhookRetryScheduler.class)
            .run(ctx -> assertThat(ctx.getBean(WebhookRetryScheduler.class).maxAttempts()).isEqualTo(3));
    }

    @Test
    void schedulerAgotaLosIntentosConfigurados() {
        WebhookDeliveryClient client = mock(WebhookDeliveryClient.class);
        WebhookRetryScheduler scheduler = new WebhookRetryScheduler(client, new long[] {0, 0});

        scheduler.scheduleRetries(EVENT);

        assertThat(scheduler.maxAttempts()).isEqualTo(3);
        verify(client).deliver(EVENT, 2);
        verify(client).deliver(EVENT, 3);
        verify(client, never()).deliver(eq(EVENT), eq(4));
    }
}
