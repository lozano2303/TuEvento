package com.capysoft.commerce.payment.application.usecase;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.commerce.payment.domain.model.Payment;
import com.capysoft.commerce.payment.domain.model.PaymentStatus;
import com.capysoft.commerce.payment.domain.repository.PaymentRepository;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;

@DisplayName("RefundRejectedSaleUseCaseImpl — compensación de la saga")
class RefundRejectedSaleUseCaseImplTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RequestRefundUseCaseImpl refunds = mock(RequestRefundUseCaseImpl.class);
    private final RefundRejectedSaleUseCaseImpl useCase = new RefundRejectedSaleUseCaseImpl(orders, payments, refunds);

    private void order() {
        Order order = mock(Order.class);
        when(order.getEventId()).thenReturn(3L);
        when(orders.findById(9L)).thenReturn(Optional.of(order));
    }

    private void payment(PaymentStatus status) {
        Payment payment = mock(Payment.class);
        when(payment.getPaymentId()).thenReturn(20L);
        when(payment.getStatus()).thenReturn(status);
        when(payments.findByOrderId(9L)).thenReturn(Optional.of(payment));
    }

    @Test
    @DisplayName("un pago aprobado se reembolsa a la wallet")
    void refunds() {
        order();
        payment(PaymentStatus.APPROVED);

        useCase.execute(9L, "SEAT_ALREADY_TAKEN");

        verify(refunds).executeToWallet(eq(20L), eq(3L), contains("SEAT_ALREADY_TAKEN"));
    }

    @Test
    @DisplayName("repetirlo no devuelve dos veces, y sin orden o sin pago no hace nada")
    void nothingToRefund() {
        order();
        payment(PaymentStatus.REFUNDED);
        useCase.execute(9L, "x");

        when(payments.findByOrderId(9L)).thenReturn(Optional.empty());
        useCase.execute(9L, "x");

        when(orders.findById(9L)).thenReturn(Optional.empty());
        useCase.execute(9L, "x");

        verifyNoInteractions(refunds);
    }
}
