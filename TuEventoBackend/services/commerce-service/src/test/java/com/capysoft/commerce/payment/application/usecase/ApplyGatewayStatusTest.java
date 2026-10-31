package com.capysoft.commerce.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.capysoft.commerce.payment.application.dto.GatewayPaymentEvent;
import com.capysoft.commerce.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.commerce.payment.domain.event.PaymentApprovedEvent;
import com.capysoft.commerce.payment.domain.model.Payment;
import com.capysoft.commerce.payment.domain.model.PaymentStatus;
import com.capysoft.commerce.payment.domain.repository.PaymentLogRepository;
import com.capysoft.commerce.payment.domain.repository.PaymentRepository;
import com.capysoft.commerce.payment.domain.repository.RefundRepository;
import com.capysoft.commerce.payment.domain.repository.TransactionWebhookRepository;
import com.capysoft.commerce.ticket.application.port.in.ConfirmOrderPaymentUseCase;
import com.capysoft.commerce.ticket.application.port.in.ConfirmRefundUseCase;
import com.capysoft.commerce.ticket.application.port.in.FailOrderPaymentUseCase;
import com.capysoft.commerce.ticket.domain.model.Money;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.wallet.application.usecase.ConfirmWalletPaymentUseCaseImpl;
import com.capysoft.commerce.wallet.application.usecase.ReleaseWalletPaymentUseCaseImpl;
import com.capysoft.commerce.wallet.application.usecase.ReverseWalletPaymentUseCaseImpl;

/**
 * Transiciones que aplican por igual el webhook y el job de conciliación.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessWebhookUseCaseImpl.applyGatewayStatus")
class ApplyGatewayStatusTest {

    @Mock PaymentRepository paymentRepository;
    @Mock TransactionWebhookRepository webhookRepository;
    @Mock PaymentLogRepository paymentLogRepository;
    @Mock RefundRepository refundRepository;
    @Mock OrderRepository orderRepository;
    @Mock PaymentGatewayPort paymentGatewayPort;
    @Mock ConfirmOrderPaymentUseCase confirmOrderPaymentUseCase;
    @Mock FailOrderPaymentUseCase failOrderPaymentUseCase;
    @Mock ConfirmRefundUseCase confirmRefundUseCase;
    @Mock ConfirmWalletPaymentUseCaseImpl confirmWalletPaymentUseCase;
    @Mock ReleaseWalletPaymentUseCaseImpl releaseWalletPaymentUseCase;
    @Mock ReverseWalletPaymentUseCaseImpl reverseWalletPaymentUseCase;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks ProcessWebhookUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        lenient().when(orderRepository.findById(9L)).thenReturn(Optional.of(
                Order.builder().orderId(9L).userId(7L).build()));
    }

    private static Payment payment(PaymentStatus status, Long walletTxId) {
        return Payment.builder().paymentId(1L).orderId(9L).gatewayTransactionId("gw-1").status(status)
                .amount(new Money(new BigDecimal("30000"), "COP")).walletAmountApplied(BigDecimal.ZERO)
                .walletTransactionId(walletTxId).build();
    }

    @Test
    @DisplayName("APPROVED confirma la orden con el id de pasarela, liquida la wallet y avisa")
    void approved_confirmsOrderAndWallet() {
        Payment payment = payment(PaymentStatus.PENDING, 55L);

        useCase.applyGatewayStatus(payment, "approved", "Reconciliation");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(confirmOrderPaymentUseCase).confirmOrderPayment(9L, "gw-1");
        verify(confirmWalletPaymentUseCase).execute(55L);
        verify(eventPublisher).publishEvent(any(PaymentApprovedEvent.class));
    }

    @Test
    @DisplayName("un APPROVED repetido no vuelve a confirmar la orden")
    void approvedTwice_isIgnored() {
        useCase.applyGatewayStatus(payment(PaymentStatus.APPROVED, null), "APPROVED", "Webhook: x");

        verifyNoInteractions(confirmOrderPaymentUseCase, paymentRepository);
    }

    @Test
    @DisplayName("CANCELLED falla la orden y libera la reserva de wallet")
    void cancelled_failsOrderAndReleasesWallet() {
        Payment payment = payment(PaymentStatus.PENDING, 55L);

        useCase.applyGatewayStatus(payment, "CANCELLED", "Reconciliation: expired");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ERROR);
        verify(failOrderPaymentUseCase).failOrderPayment(eq(9L), anyString());
        verify(releaseWalletPaymentUseCase).execute(55L);
        verify(confirmOrderPaymentUseCase, never()).confirmOrderPayment(any(), any());
    }

    @Test
    @DisplayName("DECLINED rechaza el pago y falla la orden")
    void declined_rejectsAndFailsOrder() {
        Payment payment = payment(PaymentStatus.PENDING, null);

        useCase.applyGatewayStatus(payment, "DECLINED", "Webhook: payment.declined");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        verify(failOrderPaymentUseCase).failOrderPayment(9L, "Payment declined");
    }

    @Test
    @DisplayName("execute: un webhook nuevo se guarda y aplica su estado al pago de esa transacción")
    void execute_newWebhook_appliesStatus() {
        Payment payment = payment(PaymentStatus.PENDING, null);
        when(paymentGatewayPort.processWebhook("{}", "sig")).thenReturn(GatewayPaymentEvent.builder()
                .eventId("evt-1").eventType("payment.declined").paymentId("gw-1").status("DECLINED").build());
        when(webhookRepository.existsByGatewayEventId("evt-1")).thenReturn(false);
        when(paymentRepository.findByGatewayTransactionId("gw-1")).thenReturn(Optional.of(payment));

        useCase.execute("{}", "sig");

        verify(webhookRepository).save(any());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    @DisplayName("execute: un webhook ya procesado se ignora")
    void execute_duplicateWebhook_isIgnored() {
        when(paymentGatewayPort.processWebhook("{}", "sig")).thenReturn(GatewayPaymentEvent.builder()
                .eventId("evt-1").paymentId("gw-1").status("APPROVED").build());
        when(webhookRepository.existsByGatewayEventId("evt-1")).thenReturn(true);

        useCase.execute("{}", "sig");

        verifyNoInteractions(paymentRepository, confirmOrderPaymentUseCase);
    }
}
