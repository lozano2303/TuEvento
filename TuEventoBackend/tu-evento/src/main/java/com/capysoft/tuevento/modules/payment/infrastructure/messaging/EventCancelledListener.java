package com.capysoft.tuevento.modules.payment.infrastructure.messaging;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.payment.application.usecase.RequestRefundUseCaseImpl;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Escucha el evento EventStatusChangedEvent y cuando un evento pasa a CANCELLED,
 * dispara reembolsos automáticos para todos los pagos aprobados asociados al evento.
 *
 * Flujo:
 * 1. Evento cambia a CANCELLED
 * 2. Buscar todas las órdenes del evento
 * 3. Buscar todos los pagos APPROVED de esas órdenes
 * 4. Disparar reembolso para cada pago
 *    - Wallet-only: Devuelve saldo a wallet inmediatamente
 *    - Gateway: Dispara reembolso en gateway (webhook completa el proceso)
 *    - Mixed: Ambos
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCancelledListener {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RequestRefundUseCaseImpl requestRefundUseCase;

    @Async
    @EventListener
    @Transactional
    public void handleEventStatusChanged(EventStatusChangedEvent event) {
        // Solo procesar si el nuevo estado es CANCELLED
        if (!"CANCELLED".equals(event.getNewStatus())) {
            return;
        }

        log.info("=== Event CANCELLED detected: eventId={}, processing automatic refunds ===", 
                event.getEventId());

        try {
            // 1. Buscar todas las órdenes del evento
            List<Order> orders = orderRepository.findByEventId(event.getEventId());
            
            if (orders.isEmpty()) {
                log.info("No orders found for cancelled event: eventId={}", event.getEventId());
                return;
            }

            log.info("Found {} orders for cancelled event: eventId={}", orders.size(), event.getEventId());

            // 2. Buscar todos los pagos APPROVED de esas órdenes
            List<Long> orderIds = orders.stream()
                    .map(Order::getOrderId)
                    .collect(Collectors.toList());

            List<Payment> approvedPayments = paymentRepository.findByOrderIdInAndStatus(
                    orderIds, PaymentStatus.APPROVED);

            if (approvedPayments.isEmpty()) {
                log.info("No approved payments to refund for cancelled event: eventId={}", event.getEventId());
                return;
            }

            log.info("Found {} approved payments to refund for cancelled event: eventId={}", 
                    approvedPayments.size(), event.getEventId());

            // 3. Disparar reembolso para cada pago
            int successCount = 0;
            int failureCount = 0;

            for (Payment payment : approvedPayments) {
                try {
                    String reason = String.format("Automatic refund: Event %d was cancelled", 
                            event.getEventId());
                    
                    log.info("Processing refund for payment: paymentId={}, orderId={}, eventId={}, " +
                            "isWalletOnly={}, walletAmount={}, gatewayAmount={}", 
                            payment.getPaymentId(), 
                            payment.getOrderId(), 
                            event.getEventId(),
                            payment.isWalletOnly(),
                            payment.getWalletAmountApplied(),
                            payment.getAmount() != null ? payment.getAmount().getAmount() : "N/A");

                    requestRefundUseCase.execute(payment.getPaymentId(), reason);
                    
                    successCount++;
                    log.info("Refund successfully initiated: paymentId={}, orderId={}", 
                            payment.getPaymentId(), payment.getOrderId());
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to initiate refund: paymentId={}, orderId={}, eventId={}", 
                            payment.getPaymentId(), payment.getOrderId(), event.getEventId(), e);
                    // Continuar con los demás pagos aunque uno falle
                }
            }

            log.info("=== Event cancellation refunds completed: eventId={}, total={}, success={}, failed={} ===",
                    event.getEventId(), approvedPayments.size(), successCount, failureCount);

        } catch (Exception e) {
            log.error("Unexpected error processing event cancellation refunds: eventId={}", 
                    event.getEventId(), e);
        }
    }
}
