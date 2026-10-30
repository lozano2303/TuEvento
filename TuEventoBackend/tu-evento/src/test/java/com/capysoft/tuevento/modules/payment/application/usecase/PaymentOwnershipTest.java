package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort;
import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.tuevento.modules.payment.application.dto.request.CreatePaymentRequest;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentLogRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.RefundRepository;
import com.capysoft.tuevento.modules.ticket.application.dto.response.RefundableTicketsSummary;
import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmRefundUseCase;
import com.capysoft.tuevento.modules.ticket.application.port.in.FailOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.application.port.in.InitiateOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.wallet.application.usecase.CreditWalletUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.GetWalletUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReleaseWalletPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReserveWalletPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReverseWalletPaymentUseCaseImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pagar o consultar un pago exige ser el comprador de la orden; reembolsar exige
 * ser admin u organizador del evento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payment ownership")
class PaymentOwnershipTest {

    private static final Long BUYER_ID = 7L;
    private static final Long OTHER_USER_ID = 8L;
    private static final Long ORGANIZER_ID = 30L;
    private static final Long ORDER_ID = 100L;
    private static final Long PAYMENT_ID = 400L;
    private static final Long EVENT_ID = 300L;

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private InitiateOrderPaymentUseCase initiateOrderPaymentUseCase;
    @Mock private ChargeOrderViaGatewayUseCase chargeOrderViaGatewayUseCase;
    @Mock private CreateWalletOnlyPaymentUseCase createWalletOnlyPaymentUseCase;
    @Mock private GetWalletUseCaseImpl getWalletUseCase;
    @Mock private ReserveWalletPaymentUseCaseImpl reserveWalletPaymentUseCase;
    @Mock private ReleaseWalletPaymentUseCaseImpl releaseWalletPaymentUseCase;
    @Mock private FailOrderPaymentUseCase failOrderPaymentUseCase;
    @Mock private RefundRepository refundRepository;
    @Mock private PaymentLogRepository paymentLogRepository;
    @Mock private PaymentGatewayPort paymentGatewayPort;
    @Mock private ConfirmRefundUseCase confirmRefundUseCase;
    @Mock private ReverseWalletPaymentUseCaseImpl reverseWalletPaymentUseCase;
    @Mock private CreditWalletUseCaseImpl creditWalletUseCase;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EventLookupPort eventLookupPort;

    @InjectMocks private CreatePaymentUseCaseImpl createPayment;
    @InjectMocks private GetPaymentUseCaseImpl getPayment;
    @InjectMocks private RequestRefundUseCaseImpl requestRefund;

    private static Order order() {
        return Order.builder().orderId(ORDER_ID).userId(BUYER_ID).eventId(EVENT_ID).build();
    }

    private static Payment approvedPayment() {
        return Payment.builder().paymentId(PAYMENT_ID).orderId(ORDER_ID).status(PaymentStatus.APPROVED).build();
    }

    @Test
    @DisplayName("Pay another user's order with wallet credit → denied before touching the order or the wallet")
    void createPayment_otherUser_isDenied() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
        CreatePaymentRequest request = new CreatePaymentRequest(ORDER_ID, "QR", true);

        assertThatThrownBy(() -> createPayment.execute(request, OTHER_USER_ID))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(initiateOrderPaymentUseCase, getWalletUseCase, reserveWalletPaymentUseCase,
                chargeOrderViaGatewayUseCase);
    }

    @Test
    @DisplayName("GET payment of another user's order → denied")
    void getPayment_otherUser_isDenied() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approvedPayment()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));

        assertThatThrownBy(() -> getPayment.execute(PAYMENT_ID, OTHER_USER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("GET own payment → returned")
    void getPayment_buyer_isAllowed() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approvedPayment()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));

        assertThat(getPayment.execute(PAYMENT_ID, BUYER_ID)).isNotNull();
    }

    @Nested
    @DisplayName("Refund requested through the API")
    class Refund {

        @Test
        @DisplayName("Organizer of another event → denied and no refund is created")
        void refund_otherOrganizer_isDenied() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approvedPayment()));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
            when(eventLookupPort.find(EVENT_ID)).thenReturn(Optional.of(new EventRef(EVENT_ID, ORGANIZER_ID, null, null)));

            assertThatThrownBy(() -> requestRefund.execute(PAYMENT_ID, "reason", OTHER_USER_ID, false))
                    .isInstanceOf(AccessDeniedException.class);
            verify(refundRepository, never()).save(any());
            verify(paymentGatewayPort, never()).refundPayment(any());
        }

        @Test
        @DisplayName("Organizer of the event → authorized and the refund proceeds to the gateway")
        void refund_eventOwner_proceeds() {
            Payment approved = Payment.builder().paymentId(PAYMENT_ID).orderId(ORDER_ID)
                    .status(PaymentStatus.APPROVED).gatewayTransactionId("gw-tx-1")
                    .amount(new Money(new BigDecimal("80000"), "COP"))
                    .walletAmountApplied(BigDecimal.ZERO)
                    .build();
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(approved));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
            when(eventLookupPort.find(EVENT_ID)).thenReturn(Optional.of(new EventRef(EVENT_ID, ORGANIZER_ID, null, null)));
            when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // Ningún ticket usado: reembolso completo por la pasarela
            when(confirmRefundUseCase.getRefundableTickets(ORDER_ID))
                    .thenReturn(new RefundableTicketsSummary(new BigDecimal("80000"), "COP", 2, 2));

            requestRefund.execute(PAYMENT_ID, "reason", ORGANIZER_ID, false);

            verify(paymentGatewayPort).refundPayment("gw-tx-1");
        }

        @Test
        @DisplayName("Admin → skips the event ownership check")
        void refund_admin_skipsOwnershipCheck() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(
                    Payment.builder().paymentId(PAYMENT_ID).orderId(ORDER_ID).status(PaymentStatus.ERROR).build()));

            // El estado ERROR corta el flujo después de la autorización: basta para ver que no se consultó el evento
            assertThatThrownBy(() -> requestRefund.execute(PAYMENT_ID, "reason", OTHER_USER_ID, true))
                    .isInstanceOf(IllegalStateException.class);
            verify(eventLookupPort, never()).find(anyLong());
        }
    }
}
