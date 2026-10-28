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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
 * 4. Reembolsar cada pago como crédito en la wallet del comprador (se crea si no
 *    existe), incluida la parte pagada por pasarela — ver RequestRefundUseCaseImpl#executeToWallet
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCancelledListener {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RequestRefundUseCaseImpl requestRefundUseCase;

    /**
     * Escucha la cancelación de un evento y dispara reembolsos automáticos.
     *
     * <p>Usa {@code @TransactionalEventListener(AFTER_COMMIT)} para que los reembolsos
     * solo se procesen si la cancelación del evento se confirmó en base de datos: con
     * {@code @EventListener} se dispararían aunque la cancelación hiciera rollback.
     * {@code @Async} los saca del hilo de la petición, porque llaman a la pasarela.
     *
     * <p>Si {@code requestRefundUseCase.executeToWallet} falla, se loguea el error
     * y se continúa con los demás pagos. En una futura migración a Kafka + outbox
     * este método se reemplazará por un consumidor del evento {@code EventStatusChanged}.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
                    String reason = String.format("Reembolso por cancelación del evento #%d",
                            event.getEventId());
                    
                    log.info("Processing refund for payment: paymentId={}, orderId={}, eventId={}, " +
                            "isWalletOnly={}, walletAmount={}, gatewayAmount={}", 
                            payment.getPaymentId(), 
                            payment.getOrderId(), 
                            event.getEventId(),
                            payment.isWalletOnly(),
                            payment.getWalletAmountApplied(),
                            payment.getAmount() != null ? payment.getAmount().getAmount() : "N/A");

                    requestRefundUseCase.executeToWallet(payment.getPaymentId(), event.getEventId(), reason);
                    
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
