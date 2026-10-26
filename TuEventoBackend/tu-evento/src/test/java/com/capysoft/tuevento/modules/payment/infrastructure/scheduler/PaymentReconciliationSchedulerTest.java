package com.capysoft.tuevento.modules.payment.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.capysoft.tuevento.modules.payment.application.dto.GatewayPayment;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.application.usecase.ProcessWebhookUseCaseImpl;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationScheduler")
class PaymentReconciliationSchedulerTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentGatewayPort paymentGatewayPort;
    @Mock ProcessWebhookUseCaseImpl processWebhookUseCase;
    @InjectMocks PaymentReconciliationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "pendingThresholdMinutes", 15);
        ReflectionTestUtils.setField(scheduler, "expireAfterMinutes", 30);
    }

    private static Payment pending(String gatewayTxId, int minutesAgo) {
        return Payment.builder().paymentId(1L).orderId(9L).gatewayTransactionId(gatewayTxId)
                .status(PaymentStatus.PENDING).createdAt(LocalDateTime.now().minusMinutes(minutesAgo)).build();
    }

    private void gatewayReports(String status) {
        when(paymentGatewayPort.getPayment("tx-1"))
                .thenReturn(GatewayPayment.builder().paymentId("tx-1").status(status).build());
    }

    @Test
    @DisplayName("un pago aprobado en la pasarela recibe la misma transición que el webhook")
    void approvedInGateway_appliesWebhookTransition() {
        Payment payment = pending("tx-1", 20);
        when(paymentRepository.findPendingOlderThan(any())).thenReturn(List.of(payment));
        gatewayReports("approved");

        scheduler.reconcilePendingPayments();

        verify(processWebhookUseCase).applyGatewayStatus(eq(payment), eq("APPROVED"), anyString());
    }

    @Test
    @DisplayName("un pago aún pendiente y reciente se deja para el siguiente ciclo")
    void stillPending_isLeftAlone() {
        when(paymentRepository.findPendingOlderThan(any())).thenReturn(List.of(pending("tx-1", 20)));
        gatewayReports("PENDING");

        scheduler.reconcilePendingPayments();

        verify(paymentGatewayPort, never()).cancelPayment(anyString());
        verifyNoInteractions(processWebhookUseCase);
    }

    @Test
    @DisplayName("un pago pendiente que expiró se cancela en la pasarela y se aplica CANCELLED")
    void expiredPending_isCancelled() {
        Payment payment = pending("tx-1", 45);
        when(paymentRepository.findPendingOlderThan(any())).thenReturn(List.of(payment));
        gatewayReports("PENDING");

        scheduler.reconcilePendingPayments();

        verify(paymentGatewayPort).cancelPayment("tx-1");
        verify(processWebhookUseCase).applyGatewayStatus(eq(payment), eq("CANCELLED"), anyString());
    }

    @Test
    @DisplayName("si la cancelación falla (p. ej. ya se pagó) no aplica nada y reintenta después")
    void cancelFails_appliesNothing() {
        when(paymentRepository.findPendingOlderThan(any())).thenReturn(List.of(pending("tx-1", 45)));
        gatewayReports("PENDING");
        doThrow(new IllegalStateException("already approved")).when(paymentGatewayPort).cancelPayment("tx-1");

        scheduler.reconcilePendingPayments();

        verifyNoInteractions(processWebhookUseCase);
    }

    @Test
    @DisplayName("un pago sin id de pasarela no se consulta, y desactivado no hace nada")
    void skipsWithoutGatewayId_andWhenDisabled() {
        when(paymentRepository.findPendingOlderThan(any())).thenReturn(List.of(pending(null, 45)));
        scheduler.reconcilePendingPayments();
        verifyNoInteractions(paymentGatewayPort, processWebhookUseCase);

        ReflectionTestUtils.setField(scheduler, "enabled", false);
        scheduler.reconcilePendingPayments();
        verify(paymentRepository).findPendingOlderThan(any());
    }
}
