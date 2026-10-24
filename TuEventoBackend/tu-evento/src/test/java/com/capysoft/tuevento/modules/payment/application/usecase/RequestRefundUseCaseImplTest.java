package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.domain.event.PaymentRefundedEvent;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentNotFoundException;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.model.Refund;
import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentLogRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.RefundRepository;
import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmRefundUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.wallet.application.usecase.CreditWalletUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReverseWalletPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReferenceEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestRefundUseCaseImpl")
class RequestRefundUseCaseImplTest {

    private static final Long PAYMENT_ID = 50L;
    private static final Long ORDER_ID   = 20L;
    private static final Long USER_ID    = 7L;
    private static final Long EVENT_ID   = 12L;
    private static final Long WALLET_TX  = 300L;
    private static final String REASON   = "Evento cancelado";

    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private PaymentLogRepository paymentLogRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentGatewayPort paymentGatewayPort;
    @Mock private ConfirmRefundUseCase confirmRefundUseCase;
    @Mock private ReverseWalletPaymentUseCaseImpl reverseWalletPaymentUseCase;
    @Mock private CreditWalletUseCaseImpl creditWalletUseCase;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private RequestRefundUseCaseImpl useCase;

    private static Payment payment(PaymentStatus status, String gatewayAmount, String walletAmount) {
        return Payment.builder()
                .paymentId(PAYMENT_ID).orderId(ORDER_ID).status(status)
                .gatewayTransactionId("gw-tx-1")
                .amount(new Money(new BigDecimal(gatewayAmount), "COP"))
                .walletAmountApplied(new BigDecimal(walletAmount))
                .walletTransactionId(WALLET_TX)
                .build();
    }

    private static Order order() {
        return Order.builder().orderId(ORDER_ID).userId(USER_ID).eventId(EVENT_ID).build();
    }

    @BeforeEach
    void saveReturnsArgument() {
        lenient().when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("executeToWallet (cancelación de evento)")
    class ExecuteToWallet {

        @Test
        @DisplayName("pago mixto → revierte la parte wallet y acredita la parte pasarela en la wallet")
        void mixedPayment_reversesWalletAndCreditsGatewayAmount() {
            Payment payment = payment(PaymentStatus.APPROVED, "80000", "20000");
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));

            useCase.executeToWallet(PAYMENT_ID, EVENT_ID, REASON);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(paymentRepository).save(payment);
            verify(paymentLogRepository).save(any());
            verify(confirmRefundUseCase).confirmRefund(ORDER_ID);
            verify(reverseWalletPaymentUseCase).execute(eq(WALLET_TX), anyString());
            verify(creditWalletUseCase).execute(USER_ID, new BigDecimal("80000"),
                    WalletReferenceEntityType.EVENT_CANCELLATION, EVENT_ID, REASON,
                    "event-cancel-refund-payment-" + PAYMENT_ID);
            verifyNoInteractions(paymentGatewayPort);

            ArgumentCaptor<Refund> refunds = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository, times(2)).save(refunds.capture());
            assertThat(refunds.getAllValues()).extracting(Refund::getStatus)
                    .containsExactly(RefundStatus.REQUESTED, RefundStatus.PROCESSED);

            ArgumentCaptor<PaymentRefundedEvent> event = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
            verify(eventPublisher).publishEvent(event.capture());
            assertThat(event.getValue().getUserId()).isEqualTo(USER_ID.intValue());
        }

        @Test
        @DisplayName("pago 100% wallet → no acredita nada extra")
        void walletOnlyPayment_doesNotCreditGatewayAmount() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "0", "20000")));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));

            useCase.executeToWallet(PAYMENT_ID, EVENT_ID, REASON);

            verify(reverseWalletPaymentUseCase).execute(eq(WALLET_TX), anyString());
            verifyNoInteractions(creditWalletUseCase);
        }

        @Test
        @DisplayName("pago no aprobado → IllegalStateException sin cambios")
        void notApproved_throws() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.PENDING, "80000", "0")));

            assertThatThrownBy(() -> useCase.executeToWallet(PAYMENT_ID, EVENT_ID, REASON))
                    .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(refundRepository, creditWalletUseCase, confirmRefundUseCase);
        }

        @Test
        @DisplayName("pago inexistente → PaymentNotFoundException")
        void missingPayment_throws() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.executeToWallet(PAYMENT_ID, EVENT_ID, REASON))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("orden inexistente → IllegalStateException sin crear el reembolso")
        void missingOrder_throws() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "80000", "0")));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.executeToWallet(PAYMENT_ID, EVENT_ID, REASON))
                    .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(refundRepository, creditWalletUseCase);
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("pago con pasarela → pide el reembolso al gateway sin cerrar localmente")
        void gatewayPayment_callsGateway() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "80000", "0")));

            useCase.execute(PAYMENT_ID, REASON);

            verify(paymentGatewayPort).refundPayment("gw-tx-1");
            verifyNoInteractions(confirmRefundUseCase, reverseWalletPaymentUseCase);
        }

        @Test
        @DisplayName("pago 100% wallet → cierra localmente y revierte la wallet")
        void walletOnlyPayment_closesLocally() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "0", "20000")));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            useCase.execute(PAYMENT_ID, REASON);

            verify(confirmRefundUseCase).confirmRefund(ORDER_ID);
            verify(reverseWalletPaymentUseCase).execute(eq(WALLET_TX), anyString());
            verify(eventPublisher, never()).publishEvent(any());
            verify(paymentGatewayPort, never()).refundPayment(anyString());
            verify(creditWalletUseCase, never()).execute(anyLong(), any(), any(), anyLong(), anyString(), anyString());
        }
    }
}
