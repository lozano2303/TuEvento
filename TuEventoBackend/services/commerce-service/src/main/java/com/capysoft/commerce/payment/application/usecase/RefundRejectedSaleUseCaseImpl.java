package com.capysoft.commerce.payment.application.usecase;

import org.springframework.stereotype.Service;

import com.capysoft.commerce.payment.application.port.in.RefundRejectedSaleUseCase;
import com.capysoft.commerce.payment.domain.model.Payment;
import com.capysoft.commerce.payment.domain.model.PaymentStatus;
import com.capysoft.commerce.payment.domain.repository.PaymentRepository;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Compensación de la saga de compra: la orden se pagó pero sus sillas ya no estaban. Reutiliza el
 * reembolso a wallet de un evento cancelado, que también anula las entradas y avisa al comprador.
 * Repetirlo no devuelve dos veces: solo actúa si el pago sigue APPROVED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundRejectedSaleUseCaseImpl implements RefundRejectedSaleUseCase {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RequestRefundUseCaseImpl requestRefundUseCase;

    @Override
    public void execute(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId).orElse(null);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (order == null || payment == null || payment.getStatus() != PaymentStatus.APPROVED) {
            log.warn("Rejected sale of order {} has nothing to refund (payment {})", orderId,
                    payment == null ? null : payment.getStatus());
            return;
        }
        requestRefundUseCase.executeToWallet(payment.getPaymentId(), order.getEventId(),
                "Reembolso: las sillas de la orden #" + orderId + " ya no estaban disponibles (" + reason + ")");
        log.warn("Order {} refunded to wallet: its seats could not be sold ({})", orderId, reason);
    }
}
