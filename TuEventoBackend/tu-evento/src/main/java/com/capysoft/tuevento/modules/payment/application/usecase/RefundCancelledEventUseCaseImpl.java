package com.capysoft.tuevento.modules.payment.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.payment.application.port.in.RefundCancelledEventUseCase;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Flujo:
 * 1. Buscar todas las órdenes del evento
 * 2. Buscar todos los pagos APPROVED de esas órdenes
 * 3. Reembolsar cada pago como crédito en la wallet del comprador (se crea si no
 *    existe), incluida la parte pagada por pasarela — ver RequestRefundUseCaseImpl#executeToWallet
 *
 * <p>Si un reembolso falla, se loguea el error y se continúa con los demás pagos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundCancelledEventUseCaseImpl implements RefundCancelledEventUseCase {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RequestRefundUseCaseImpl requestRefundUseCase;

    @Override
    public void execute(Long eventId) {
        log.info("=== Event CANCELLED detected: eventId={}, processing automatic refunds ===", eventId);

        List<Long> orderIds = orderRepository.findByEventId(eventId).stream()
                .map(Order::getOrderId)
                .toList();
        if (orderIds.isEmpty()) {
            log.info("No orders found for cancelled event: eventId={}", eventId);
            return;
        }

        List<Payment> approvedPayments = paymentRepository.findByOrderIdInAndStatus(orderIds, PaymentStatus.APPROVED);
        if (approvedPayments.isEmpty()) {
            log.info("No approved payments to refund for cancelled event: eventId={}", eventId);
            return;
        }

        String reason = String.format("Reembolso por cancelación del evento #%d", eventId);
        int failureCount = 0;
        for (Payment payment : approvedPayments) {
            try {
                requestRefundUseCase.executeToWallet(payment.getPaymentId(), eventId, reason);
                log.info("Refund successfully initiated: paymentId={}, orderId={}",
                        payment.getPaymentId(), payment.getOrderId());
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to initiate refund: paymentId={}, orderId={}, eventId={}",
                        payment.getPaymentId(), payment.getOrderId(), eventId, e);
            }
        }

        log.info("=== Event cancellation refunds completed: eventId={}, total={}, failed={} ===",
                eventId, approvedPayments.size(), failureCount);
    }
}
