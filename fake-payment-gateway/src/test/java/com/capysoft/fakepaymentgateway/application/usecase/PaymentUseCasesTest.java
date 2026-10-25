package com.capysoft.fakepaymentgateway.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.fakepaymentgateway.application.dto.request.CreatePaymentRequest;
import com.capysoft.fakepaymentgateway.application.dto.response.PaymentResponse;
import com.capysoft.fakepaymentgateway.application.port.out.WebhookNotifierPort;
import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.domain.model.InvalidStatusTransitionException;
import com.capysoft.fakepaymentgateway.domain.model.Money;
import com.capysoft.fakepaymentgateway.domain.model.Payment;
import com.capysoft.fakepaymentgateway.domain.model.PaymentMethod;
import com.capysoft.fakepaymentgateway.domain.model.PaymentStatus;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentUseCasesTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private WebhookNotifierPort notifier;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.create("ORDER-1", new Money(new BigDecimal("50000"), "COP"), PaymentMethod.PSE);
    }

    private void givenStoredPayment() {
        when(repository.findByPaymentId(payment.getPaymentId())).thenReturn(Optional.of(payment));
    }

    private List<PaymentStatus> notifiedStatuses(int expectedCalls) {
        ArgumentCaptor<PaymentStatusChanged> captor = ArgumentCaptor.forClass(PaymentStatusChanged.class);
        verify(notifier, times(expectedCalls)).notify(captor.capture());
        return captor.getAllValues().stream().map(PaymentStatusChanged::status).toList();
    }

    @Test
    void crearPagoPersisteYDevuelvePendienteSinWebhook() {
        when(repository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        CreatePaymentRequest request = new CreatePaymentRequest("ORDER-9", new BigDecimal("1000"), "usd", PaymentMethod.QR);

        PaymentResponse response = new CreatePaymentUseCase(repository).execute(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.paymentId()).startsWith("fake_");
        verify(repository).save(any(Payment.class));
        verify(notifier, never()).notify(any());
    }

    @Test
    void consultarPagoInexistenteLanzaNotFound() {
        when(repository.findByPaymentId("fake_x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetPaymentUseCase(repository).execute("fake_x"))
            .isInstanceOf(GetPaymentUseCase.PaymentNotFoundException.class);
    }

    @Test
    void aprobarPasaPorProcesandoYNotificaAmbasTransiciones() {
        givenStoredPayment();

        new ApprovePaymentUseCase(repository, notifier).execute(payment.getPaymentId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(notifiedStatuses(2)).containsExactly(PaymentStatus.PROCESSING, PaymentStatus.APPROVED);
        verify(repository, times(2)).save(payment);
    }

    @Test
    void rechazarNotificaProcesandoYRechazado() {
        givenStoredPayment();

        new DeclinePaymentUseCase(repository, notifier).execute(payment.getPaymentId());

        assertThat(notifiedStatuses(2)).containsExactly(PaymentStatus.PROCESSING, PaymentStatus.DECLINED);
    }

    @Test
    void fallarNotificaProcesandoYFallido() {
        givenStoredPayment();

        new FailPaymentUseCase(repository, notifier).execute(payment.getPaymentId());

        assertThat(notifiedStatuses(2)).containsExactly(PaymentStatus.PROCESSING, PaymentStatus.FAILED);
    }

    @Test
    void cancelarNotificaUnaSolaTransicion() {
        givenStoredPayment();

        new CancelPaymentUseCase(repository, notifier).execute(payment.getPaymentId());

        assertThat(notifiedStatuses(1)).containsExactly(PaymentStatus.CANCELLED);
    }

    @Test
    void reembolsarPagoAprobado() {
        payment.markAsProcessing();
        payment.approve();
        givenStoredPayment();

        new RefundPaymentUseCase(repository, notifier).execute(payment.getPaymentId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(notifiedStatuses(1)).containsExactly(PaymentStatus.REFUNDED);
    }

    @Test
    void reembolsarPagoNoAprobadoNoPersisteNiNotifica() {
        givenStoredPayment();
        RefundPaymentUseCase useCase = new RefundPaymentUseCase(repository, notifier);

        assertThatThrownBy(() -> useCase.execute(payment.getPaymentId()))
            .isInstanceOf(InvalidStatusTransitionException.class);
        verify(repository, never()).save(any());
        verify(notifier, never()).notify(any());
    }

    @Test
    void accionSobrePagoInexistenteLanzaNotFound() {
        when(repository.findByPaymentId("fake_x")).thenReturn(Optional.empty());
        ApprovePaymentUseCase useCase = new ApprovePaymentUseCase(repository, notifier);

        assertThatThrownBy(() -> useCase.execute("fake_x"))
            .isInstanceOf(GetPaymentUseCase.PaymentNotFoundException.class);
        verify(notifier, never()).notify(any());
    }
}
