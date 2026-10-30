package com.capysoft.tuevento.modules.payment.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;

@DisplayName("RefundCancelledEventUseCaseImpl")
class RefundCancelledEventUseCaseImplTest {

    private static final long EVENT_ID = 9L;
    private static final String REASON = "Reembolso por cancelación del evento #9";

    private final OrderRepository orders = mock(OrderRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RequestRefundUseCaseImpl refunds = mock(RequestRefundUseCaseImpl.class);
    private final RefundCancelledEventUseCaseImpl useCase = new RefundCancelledEventUseCaseImpl(orders, payments, refunds);

    private static Order order(long orderId) {
        Order order = mock(Order.class);
        when(order.getOrderId()).thenReturn(orderId);
        return order;
    }

    private static Payment payment(long paymentId) {
        Payment payment = mock(Payment.class);
        when(payment.getPaymentId()).thenReturn(paymentId);
        return payment;
    }

    @Test
    @DisplayName("sin órdenes no consulta pagos ni reembolsa")
    void noOrders() {
        when(orders.findByEventId(EVENT_ID)).thenReturn(List.of());

        useCase.execute(EVENT_ID);

        verifyNoInteractions(payments, refunds);
    }

    @Test
    @DisplayName("sin pagos aprobados no reembolsa")
    void noApprovedPayments() {
        List<Order> eventOrders = List.of(order(1L));
        when(orders.findByEventId(EVENT_ID)).thenReturn(eventOrders);
        when(payments.findByOrderIdInAndStatus(List.of(1L), PaymentStatus.APPROVED)).thenReturn(List.of());

        useCase.execute(EVENT_ID);

        verifyNoInteractions(refunds);
    }

    @Test
    @DisplayName("si un reembolso falla, sigue con los demás pagos")
    void failureDoesNotStopTheRest() {
        List<Order> eventOrders = List.of(order(1L), order(2L));
        List<Payment> approved = List.of(payment(10L), payment(20L));
        when(orders.findByEventId(EVENT_ID)).thenReturn(eventOrders);
        when(payments.findByOrderIdInAndStatus(any(), any())).thenReturn(approved);
        doThrow(new IllegalStateException("gateway down")).when(refunds).executeToWallet(10L, EVENT_ID, REASON);

        useCase.execute(EVENT_ID);

        verify(payments).findByOrderIdInAndStatus(List.of(1L, 2L), PaymentStatus.APPROVED);
        verify(refunds).executeToWallet(10L, EVENT_ID, REASON);
        verify(refunds).executeToWallet(20L, EVENT_ID, REASON);
    }
}
