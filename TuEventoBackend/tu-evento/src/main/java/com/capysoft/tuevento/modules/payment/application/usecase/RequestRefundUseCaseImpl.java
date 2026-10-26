package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.domain.event.PaymentRefundedEvent;
import com.capysoft.tuevento.modules.payment.domain.model.*;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentLogRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.RefundRepository;
import com.capysoft.tuevento.modules.ticket.application.dto.response.RefundableTicketsSummary;
import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmRefundUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.wallet.application.usecase.CreditWalletUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReverseWalletPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReferenceEntityType;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Caso de uso para iniciar un reembolso.
 *
 * Flujo pasarela (amount > 0):
 *  1. Valida que el Payment esté en APPROVED
 *  2. Crea Refund en REQUESTED
 *  3. Llama a PaymentGatewayPort.refundPayment() — el gateway dispara webhook payment.refunded
 *  4. El webhook (ProcessWebhookUseCaseImpl) completa el cierre local
 *
 * Flujo wallet-only (amount == 0):
 *  1. Valida que el Payment esté en APPROVED
 *  2. Crea Refund en REQUESTED
 *  3. Ejecuta el cierre local directamente (sin gateway, sin webhook):
 *     Payment→REFUNDED, Refund→PROCESSED, ConfirmRefundUseCase, ReverseWalletPayment
 *
 * Flujo cancelación de evento (executeToWallet):
 *  Todo el pago vuelve como crédito a la wallet del comprador (se crea si no existe):
 *  la parte wallet se revierte y la parte pasarela se acredita con CreditWalletUseCase.
 *  No se llama al gateway, así que no hay doble devolución.
 *
 * El cierre local está extraído a closeRefundLocally() y es compartido
 * con el caso webhook de ProcessWebhookUseCaseImpl para evitar duplicación.
 *
 * Tickets usados: nunca se reembolsan. Antes de cualquier flujo se consulta qué
 * tickets de la orden siguen PAID:
 *  - Todos sin usar  → flujos anteriores (reembolso completo).
 *  - Algunos usados  → refundUnusedTicketsToWallet(): solo el precio de los tickets sin
 *    usar vuelve como crédito en la wallet (la pasarela solo admite reembolsos totales).
 *    El pago sigue APPROVED porque la parte usada sí se cobró.
 *  - Todos usados    → nada que reembolsar (la API responde error; la cancelación lo omite).
 */
@Service
@RequiredArgsConstructor
public class RequestRefundUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(RequestRefundUseCaseImpl.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final ConfirmRefundUseCase confirmRefundUseCase;
    private final ReverseWalletPaymentUseCaseImpl reverseWalletPaymentUseCase;
    private final CreditWalletUseCaseImpl creditWalletUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final EventRepository eventRepository;

    /**
     * Reembolso pedido desde la API: un administrador puede reembolsar cualquier
     * pago; un organizador, solo los pagos de órdenes de sus propios eventos.
     */
    @Transactional
    public void execute(Long paymentId, String reason, Long requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin) {
            Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
            boolean ownsEvent = orderRepository.findById(payment.getOrderId())
                .flatMap(order -> eventRepository.findById(order.getEventId()))
                .map(event -> event.getUserId().equals(requesterId))
                .orElse(false);
            if (!ownsEvent) {
                throw new AccessDeniedException("You can only refund payments of your own events");
            }
        }
        requestRefund(paymentId, reason);
    }

    @Transactional
    public void execute(Long paymentId, String reason) {
        requestRefund(paymentId, reason);
    }

    private void requestRefund(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new IllegalStateException(
                String.format("Cannot refund payment %d: current status is %s, expected APPROVED",
                    paymentId, payment.getStatus()));
        }

        RefundableTicketsSummary refundable = confirmRefundUseCase.getRefundableTickets(payment.getOrderId());
        if (refundable.isNothingToRefund()) {
            throw new BusinessException("NOTHING_TO_REFUND",
                "All tickets of this order were already used; there is nothing to refund");
        }
        if (refundable.isPartial()) {
            Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found for payment " + paymentId));
            refundUnusedTicketsToWallet(payment, order, reason,
                WalletReferenceEntityType.ORDER, order.getOrderId(), "partial-refund-payment-" + paymentId);
            return;
        }

        // Crear Refund en REQUESTED
        Refund refund = Refund.builder()
            .paymentId(paymentId)
            .status(RefundStatus.REQUESTED)
            .reason(reason)
            .requestedAt(LocalDateTime.now())
            .build();
        Refund savedRefund = refundRepository.save(refund);

        log.info("Refund requested: paymentId={}, gatewayTxId={}, isWalletOnly={}, reason={}",
            paymentId, payment.getGatewayTransactionId(), payment.isWalletOnly(), reason);

        if (payment.isWalletOnly()) {
            // Pago 100% wallet — cerrar localmente sin gateway
            closeRefundLocally(payment, savedRefund, "Wallet-only refund: " + reason);
        } else {
            // Pago con pasarela — el gateway dispara el webhook payment.refunded.
            // Se llama DESPUÉS del commit: esta transacción tiene bloqueados los tickets
            // (getRefundableTickets) y la pasarela envía el webhook antes de responder; si la
            // llamada fuera aquí dentro, el webhook esperaría esos bloqueos, la llamada expiraría
            // a los 30 s con un 500 y el rollback borraría el Refund REQUESTED.
            String gatewayTransactionId = payment.getGatewayTransactionId();
            runAfterCommit(() -> paymentGatewayPort.refundPayment(gatewayTransactionId));
        }
    }

    /** Ejecuta la acción tras el commit de la transacción actual (o ya, si no hay transacción). */
    private static void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /**
     * Reembolso por cancelación de evento: devuelve el pago completo como crédito
     * en la wallet del comprador, también la parte pagada por pasarela.
     *
     * REQUIRES_NEW: cada pago se confirma por separado, así un fallo en uno no marca
     * como rollback-only la transacción del listener y no deshace los demás reembolsos.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeToWallet(Long paymentId, Long eventId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new IllegalStateException(
                String.format("Cannot refund payment %d: current status is %s, expected APPROVED",
                    paymentId, payment.getStatus()));
        }

        Order order = orderRepository.findById(payment.getOrderId())
            .orElseThrow(() -> new IllegalStateException(
                "Order not found for payment " + paymentId));

        RefundableTicketsSummary refundable = confirmRefundUseCase.getRefundableTickets(order.getOrderId());
        if (refundable.isNothingToRefund()) {
            log.info("Event cancellation: every ticket of order {} was already used, nothing to refund (paymentId={})",
                order.getOrderId(), paymentId);
            return;
        }
        if (refundable.isPartial()) {
            refundUnusedTicketsToWallet(payment, order, reason,
                WalletReferenceEntityType.EVENT_CANCELLATION, eventId, "event-cancel-refund-payment-" + paymentId);
            return;
        }

        Refund savedRefund = refundRepository.save(Refund.builder()
            .paymentId(paymentId)
            .status(RefundStatus.REQUESTED)
            .reason(reason)
            .requestedAt(LocalDateTime.now())
            .build());

        // Parte wallet → se revierte; Order/Tickets → REFUNDED; notificación de reembolso
        closeRefundLocally(payment, savedRefund, "Event cancellation refund to wallet: " + reason);

        // Parte pasarela → crédito en wallet (idempotente por pago)
        BigDecimal gatewayAmount = payment.getAmount() != null ? payment.getAmount().getAmount() : BigDecimal.ZERO;
        if (gatewayAmount != null && gatewayAmount.compareTo(BigDecimal.ZERO) > 0) {
            creditWalletUseCase.execute(
                order.getUserId(),
                gatewayAmount,
                WalletReferenceEntityType.EVENT_CANCELLATION,
                eventId,
                reason,
                "event-cancel-refund-payment-" + paymentId);
            log.info("Gateway amount credited to wallet on event cancellation: paymentId={}, userId={}, amount={}",
                paymentId, order.getUserId(), gatewayAmount);
        }
    }

    /**
     * Reembolso parcial: la orden tiene tickets usados. Solo el precio de los tickets
     * sin usar vuelve al comprador, como crédito en su wallet (idempotente por clave).
     * Los tickets sin usar pasan a REFUNDED, los usados se conservan, y el pago sigue
     * APPROVED porque la parte usada sí se cobró.
     *
     * Se acredita exactamente lo que confirmRefund marcó como REFUNDED (no el cálculo
     * previo): los tickets quedan bloqueados en esta transacción desde getRefundableTickets,
     * así que un escaneo concurrente espera y no puede colarse entre el cálculo y el crédito.
     */
    private void refundUnusedTicketsToWallet(Payment payment, Order order, String reason,
                                             WalletReferenceEntityType referenceType,
                                             Long referenceId, String idempotencyKey) {
        RefundableTicketsSummary refundable = confirmRefundUseCase.confirmRefund(order.getOrderId());
        if (refundable.isNothingToRefund()) {
            log.warn("Partial refund skipped: no unused tickets left on order {} (paymentId={})",
                order.getOrderId(), payment.getPaymentId());
            return;
        }

        String detail = String.format("%s (reembolso parcial: %d de %d tickets sin usar)",
            reason, refundable.refundableCount(), refundable.totalCount());

        Refund requested = refundRepository.save(Refund.builder()
            .paymentId(payment.getPaymentId())
            .status(RefundStatus.REQUESTED)
            .reason(detail)
            .requestedAt(LocalDateTime.now())
            .build());

        creditWalletUseCase.execute(order.getUserId(), refundable.amount(), referenceType, referenceId,
            detail, idempotencyKey);

        refundRepository.save(Refund.builder()
            .refundId(requested.getRefundId())
            .paymentId(requested.getPaymentId())
            .status(RefundStatus.PROCESSED)
            .reason(requested.getReason())
            .requestedAt(requested.getRequestedAt())
            .processedAt(LocalDateTime.now())
            .build());

        log.info("Partial refund to wallet: paymentId={}, orderId={}, refundedTickets={}/{}, amount={}",
            payment.getPaymentId(), order.getOrderId(), refundable.refundableCount(),
            refundable.totalCount(), refundable.amount());

        try {
            eventPublisher.publishEvent(PaymentRefundedEvent.builder()
                .paymentId(payment.getPaymentId())
                .userId(order.getUserId().intValue())
                .walletAmount(refundable.amount())
                .gatewayAmount(BigDecimal.ZERO)
                .currency(refundable.currency() != null ? refundable.currency() : "COP")
                .build());
        } catch (Exception e) {
            log.error("Failed to publish PaymentRefundedEvent (non-breaking): paymentId={}",
                payment.getPaymentId(), e);
        }
    }

    /**
     * Cierre local del reembolso (pagos wallet-only y cancelación de evento).
     * Simétrico al bloque REFUNDED de ProcessWebhookUseCaseImpl.
     */
    private void closeRefundLocally(Payment payment, Refund refund, String reason) {
        PaymentStatus oldStatus = payment.getStatus();

        // Transicionar Payment a REFUNDED
        payment.refund();
        paymentRepository.save(payment);

        // Log de auditoría
        PaymentLog paymentLog = PaymentLog.builder()
            .paymentId(payment.getPaymentId())
            .oldStatus(oldStatus)
            .newStatus(PaymentStatus.REFUNDED)
            .changedAt(LocalDateTime.now())
            .reason(reason)
            .build();
        paymentLogRepository.save(paymentLog);

        // Actualizar Refund a PROCESSED
        Refund processed = Refund.builder()
            .refundId(refund.getRefundId())
            .paymentId(refund.getPaymentId())
            .status(RefundStatus.PROCESSED)
            .reason(refund.getReason())
            .requestedAt(refund.getRequestedAt())
            .processedAt(LocalDateTime.now())
            .build();
        refundRepository.save(processed);

        // Transicionar Order y Tickets a REFUNDED en módulo ticket
        confirmRefundUseCase.confirmRefund(payment.getOrderId());

        // Devolver crédito wallet
        BigDecimal walletApplied = payment.getWalletAmountApplied();
        if (walletApplied != null && walletApplied.compareTo(BigDecimal.ZERO) > 0
                && payment.getWalletTransactionId() != null) {
            reverseWalletPaymentUseCase.execute(
                payment.getWalletTransactionId(),
                reason);
            log.info("Wallet credit reversed on wallet-only refund: txId={}, amount={}",
                payment.getWalletTransactionId(), walletApplied);
        }

        // Publicar evento para notificaciones
        try {
            Order order = orderRepository.findById(payment.getOrderId()).orElse(null);
            if (order != null) {
                PaymentRefundedEvent refundedEvent = PaymentRefundedEvent.builder()
                        .paymentId(payment.getPaymentId())
                        .userId(order.getUserId().intValue())
                        .walletAmount(payment.getWalletAmountApplied())
                        .gatewayAmount(payment.getAmount() != null ? payment.getAmount().getAmount() : BigDecimal.ZERO)
                        .currency(payment.getAmount() != null ? payment.getAmount().getCurrency().toString() : "USD")
                        .build();
                eventPublisher.publishEvent(refundedEvent);
                log.debug("PaymentRefundedEvent published for wallet-only refund: paymentId={}", payment.getPaymentId());
            } else {
                log.warn("Order not found for payment {}, PaymentRefundedEvent not published", payment.getPaymentId());
            }
        } catch (Exception e) {
            log.error("Failed to publish PaymentRefundedEvent (non-breaking): paymentId={}", 
                    payment.getPaymentId(), e);
        }

        log.info("Wallet-only refund closed locally: paymentId={}, orderId={}",
            payment.getPaymentId(), payment.getOrderId());
    }
}
