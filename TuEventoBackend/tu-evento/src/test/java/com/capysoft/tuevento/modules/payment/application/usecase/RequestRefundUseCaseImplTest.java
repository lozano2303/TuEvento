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
import com.capysoft.tuevento.modules.ticket.application.dto.response.RefundableTicketsSummary;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        // Por defecto ningún ticket se usó: reembolso completo (flujos anteriores)
        lenient().when(confirmRefundUseCase.getRefundableTickets(any()))
                .thenReturn(new RefundableTicketsSummary(new BigDecimal("100000"), "COP", 2, 2));
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
            // Solo consulta los tickets reembolsables; el cierre local lo hace el webhook
            verify(confirmRefundUseCase, never()).confirmRefund(any());
            verifyNoInteractions(reverseWalletPaymentUseCase);
        }

        @Test
        @DisplayName("dentro de una transacción, el gateway se llama recién después del commit (sin interbloqueo con el webhook)")
        void gatewayIsCalledAfterCommit() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "80000", "0")));
            TransactionSynchronizationManager.initSynchronization();
            try {
                useCase.execute(PAYMENT_ID, REASON, 1L, true);

                // Mientras la transacción sigue abierta (tickets bloqueados) no se llama al gateway
                verify(paymentGatewayPort, never()).refundPayment(anyString());
                verify(refundRepository).save(any(Refund.class));

                TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
                verify(paymentGatewayPort).refundPayment("gw-tx-1");
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("si la transacción hace rollback, no se pide el reembolso al gateway")
        void gatewayIsNotCalledOnRollback() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "80000", "0")));
            TransactionSynchronizationManager.initSynchronization();
            try {
                useCase.execute(PAYMENT_ID, REASON);
                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
                verify(paymentGatewayPort, never()).refundPayment(anyString());
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
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

    @Nested
    @DisplayName("tickets usados")
    class UsedTickets {

        private final RefundableTicketsSummary partial =
                new RefundableTicketsSummary(new BigDecimal("50000"), "COP", 1, 2);
        private final RefundableTicketsSummary allUsed =
                new RefundableTicketsSummary(BigDecimal.ZERO, "COP", 0, 2);

        @Test
        @DisplayName("todos usados → NOTHING_TO_REFUND sin crear reembolso ni llamar al gateway")
        void execute_allTicketsUsed_isRejected() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "80000", "0")));
            when(confirmRefundUseCase.getRefundableTickets(ORDER_ID)).thenReturn(allUsed);

            assertThatThrownBy(() -> useCase.execute(PAYMENT_ID, REASON))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already used");
            verify(refundRepository, never()).save(any());
            verifyNoInteractions(paymentGatewayPort, creditWalletUseCase);
        }

        @Test
        @DisplayName("parcial → solo el precio de los tickets sin usar vuelve a la wallet; el pago sigue APPROVED")
        void execute_partial_creditsOnlyUnusedTickets() {
            Payment payment = payment(PaymentStatus.APPROVED, "80000", "20000");
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
            when(confirmRefundUseCase.getRefundableTickets(ORDER_ID)).thenReturn(partial);
            when(confirmRefundUseCase.confirmRefund(ORDER_ID)).thenReturn(partial);

            useCase.execute(PAYMENT_ID, REASON);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            verify(confirmRefundUseCase).confirmRefund(ORDER_ID);
            verify(creditWalletUseCase).execute(eq(USER_ID), eq(new BigDecimal("50000")),
                    eq(WalletReferenceEntityType.ORDER), eq(ORDER_ID), anyString(),
                    eq("partial-refund-payment-" + PAYMENT_ID));
            verifyNoInteractions(paymentGatewayPort, reverseWalletPaymentUseCase);
            verify(paymentRepository, never()).save(any());

            ArgumentCaptor<Refund> refunds = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository, times(2)).save(refunds.capture());
            assertThat(refunds.getAllValues()).extracting(Refund::getStatus)
                    .containsExactly(RefundStatus.REQUESTED, RefundStatus.PROCESSED);
        }

        @Test
        @DisplayName("se acredita lo realmente reembolsado, no el cálculo previo")
        void execute_partial_creditsWhatWasActuallyRefunded() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "80000", "0")));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
            when(confirmRefundUseCase.getRefundableTickets(ORDER_ID))
                    .thenReturn(new RefundableTicketsSummary(new BigDecimal("100000"), "COP", 2, 3));
            when(confirmRefundUseCase.confirmRefund(ORDER_ID)).thenReturn(partial);

            useCase.execute(PAYMENT_ID, REASON);

            verify(creditWalletUseCase).execute(eq(USER_ID), eq(new BigDecimal("50000")),
                    eq(WalletReferenceEntityType.ORDER), eq(ORDER_ID), anyString(), anyString());
        }

        @Test
        @DisplayName("si al confirmar ya no queda ningún ticket sin usar → no acredita nada")
        void execute_partial_nothingLeftAtConfirmation_creditsNothing() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "80000", "0")));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
            when(confirmRefundUseCase.getRefundableTickets(ORDER_ID)).thenReturn(partial);
            when(confirmRefundUseCase.confirmRefund(ORDER_ID)).thenReturn(allUsed);

            useCase.execute(PAYMENT_ID, REASON);

            verifyNoInteractions(creditWalletUseCase, paymentGatewayPort);
            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("cancelación con todos usados → se omite el pago sin error")
        void executeToWallet_allTicketsUsed_isSkipped() {
            when(paymentRepository.findById(PAYMENT_ID))
                    .thenReturn(Optional.of(payment(PaymentStatus.APPROVED, "80000", "0")));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
            when(confirmRefundUseCase.getRefundableTickets(ORDER_ID)).thenReturn(allUsed);

            useCase.executeToWallet(PAYMENT_ID, EVENT_ID, REASON);

            verify(refundRepository, never()).save(any());
            verify(confirmRefundUseCase, never()).confirmRefund(any());
            verifyNoInteractions(creditWalletUseCase, reverseWalletPaymentUseCase);
        }

        @Test
        @DisplayName("cancelación parcial → acredita solo los tickets sin usar, sin revertir la wallet completa")
        void executeToWallet_partial_creditsOnlyUnusedTickets() {
            Payment payment = payment(PaymentStatus.APPROVED, "80000", "20000");
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
            when(confirmRefundUseCase.getRefundableTickets(ORDER_ID)).thenReturn(partial);
            when(confirmRefundUseCase.confirmRefund(ORDER_ID)).thenReturn(partial);

            useCase.executeToWallet(PAYMENT_ID, EVENT_ID, REASON);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            verify(creditWalletUseCase).execute(eq(USER_ID), eq(new BigDecimal("50000")),
                    eq(WalletReferenceEntityType.EVENT_CANCELLATION), eq(EVENT_ID), anyString(),
                    eq("event-cancel-refund-payment-" + PAYMENT_ID));
            verifyNoInteractions(reverseWalletPaymentUseCase, paymentGatewayPort);
        }
    }
}
