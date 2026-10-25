package com.capysoft.fakepaymentgateway.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.domain.model.Money;
import com.capysoft.fakepaymentgateway.domain.model.PaymentStatus;

/**
 * Cubre L-01 con el proxy real de Spring: @Async debe aplicarse a scheduleRetries.
 */
@SpringJUnitConfig(WebhookRetryAsyncTest.Config.class)
class WebhookRetryAsyncTest {

    private static final PaymentStatusChanged EVENT = new PaymentStatusChanged(
        "evt-1", "fake_1", PaymentStatus.APPROVED, new Money(BigDecimal.TEN, "COP"), Instant.now());

    @Configuration
    @EnableAsync
    static class Config {
        @Bean
        WebhookDeliveryClient deliveryClient() {
            return mock(WebhookDeliveryClient.class);
        }

        @Bean
        WebhookRetryScheduler retryScheduler(WebhookDeliveryClient deliveryClient) {
            return new WebhookRetryScheduler(deliveryClient, new long[] {300, 300});
        }
    }

    @Autowired
    WebhookRetryScheduler scheduler;

    @Autowired
    WebhookDeliveryClient client;

    @Test
    void losReintentosCorrenEnOtroHiloSinBloquearAlLlamador() {
        AtomicReference<String> retryThread = new AtomicReference<>();
        when(client.deliver(any(), anyInt())).thenAnswer(inv -> {
            retryThread.set(Thread.currentThread().getName());
            return false;
        });

        long start = System.nanoTime();
        scheduler.scheduleRetries(EVENT);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Con backoff de 300 ms + 300 ms, una llamada síncrona tardaría >= 600 ms
        assertThat(elapsedMs).isLessThan(200);
        verify(client, timeout(3000)).deliver(EVENT, 3);
        assertThat(retryThread.get()).isNotEqualTo(Thread.currentThread().getName());
    }
}
