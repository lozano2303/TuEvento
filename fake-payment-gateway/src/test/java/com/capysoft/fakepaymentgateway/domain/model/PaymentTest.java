package com.capysoft.fakepaymentgateway.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;

class PaymentTest {

    private Payment newPayment() {
        return Payment.create("ORDER-1", new Money(new BigDecimal("150000"), "cop"), PaymentMethod.CARD);
    }

    @Test
    void crearPagoLoDejaPendienteConIdentificadores() {
        Payment payment = newPayment();

        assertThat(payment.getPaymentId()).startsWith("fake_").hasSize(41);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getMoney().getCurrency()).isEqualTo("COP");
        assertThat(payment.getWebhookEventId()).isNotBlank();
        assertThat(payment.getCreatedAt()).isNotNull();
    }

    @Test
    void cadaTransicionGeneraUnEventIdNuevo() {
        Payment payment = newPayment();
        String initialEventId = payment.getWebhookEventId();

        PaymentStatusChanged processing = payment.markAsProcessing();
        PaymentStatusChanged approved = payment.approve();

        assertThat(processing.eventId()).isNotEqualTo(initialEventId);
        assertThat(approved.eventId()).isNotEqualTo(processing.eventId());
        assertThat(approved.eventId()).isEqualTo(payment.getWebhookEventId());
    }

    @Test
    void elEventoReflejaElEstadoYElMonto() {
        Payment payment = newPayment();

        PaymentStatusChanged event = payment.cancel();

        assertThat(event.paymentId()).isEqualTo(payment.getPaymentId());
        assertThat(event.status()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(event.money()).isEqualTo(payment.getMoney());
        assertThat(event.timestamp()).isEqualTo(payment.getUpdatedAt());
    }

    @Test
    void flujoCompletoAprobarYReembolsar() {
        Payment payment = newPayment();
        payment.markAsProcessing();
        payment.approve();
        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void transicionInvalidaLanzaExcepcionYNoCambiaElEstado() {
        Payment payment = newPayment();
        String eventIdBefore = payment.getWebhookEventId();

        assertThatThrownBy(payment::refund)
            .isInstanceOf(InvalidStatusTransitionException.class)
            .hasMessage("Cannot transition from PENDING to REFUNDED");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getWebhookEventId()).isEqualTo(eventIdBefore);
    }

    @Test
    void noSePuedeAprobarSinPasarPorProcesando() {
        Payment payment = newPayment();

        assertThatThrownBy(payment::approve).isInstanceOf(InvalidStatusTransitionException.class);
    }
}
