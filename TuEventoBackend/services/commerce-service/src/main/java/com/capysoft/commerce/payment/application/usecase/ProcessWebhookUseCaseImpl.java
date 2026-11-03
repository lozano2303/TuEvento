package com.capysoft.commerce.payment.application.usecase;

import com.capysoft.commerce.payment.application.dto.GatewayPaymentEvent;
import com.capysoft.commerce.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.commerce.payment.domain.event.PaymentApprovedEvent;
import com.capysoft.commerce.payment.domain.event.PaymentRefundedEvent;
import com.capysoft.commerce.payment.domain.model.*;
import com.capysoft.commerce.payment.domain.repository.PaymentLogRepository;
import com.capysoft.commerce.payment.domain.repository.PaymentRepository;
import com.capysoft.commerce.payment.domain.repository.RefundRepository;
import com.capysoft.commerce.payment.domain.repository.TransactionWebhookRepository;
import com.capysoft.commerce.ticket.application.port.in.ConfirmOrderPaymentUseCase;
import com.capysoft.commerce.ticket.application.port.in.ConfirmRefundUseCase;
import com.capysoft.commerce.ticket.application.port.in.FailOrderPaymentUseCase;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.wallet.application.usecase.ConfirmWalletPaymentUseCaseImpl;
import com.capysoft.commerce.wallet.application.usecase.ReleaseWalletPaymentUseCaseImpl;
import com.capysoft.commerce.wallet.application.usecase.ReverseWalletPaymentUseCaseImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Processes payment gateway webhooks.
 * 1. Verifies HMAC signature
 * 2. Checks idempotency (gatewayEventId)
 * 3. Persists TransactionWebhook
 * 4. Updates Payment with PaymentLog
 * 5. Triggers ticket and wallet side effects
 *
 * Wallet rules (only when Payment.walletTransactionId != null):
 *   APPROVED  → ConfirmWalletPayment (settles the reserved balance)
 *   DECLINED/FAILED/CANCELLED → ReleaseWalletPayment (releases PENDING reservation)
 *   REFUNDED + walletAmountApplied > 0 → ReverseWalletPayment (returns credit to buyer)
 *
 * Organizer earnings are informational only — computed on demand from approved
 * payments via GET /api/v1/organizer/earnings. No wallet credit is made here.
 */
@Service
@RequiredArgsConstructor
public class ProcessWebhookUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(ProcessWebhookUseCaseImpl.class);

    private final PaymentRepository paymentRepository;
    private final TransactionWebhookRepository webhookRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final ConfirmOrderPaymentUseCase confirmOrderPaymentUseCase;
    private final FailOrderPaymentUseCase failOrderPaymentUseCase;
    private final ConfirmRefundUseCase confirmRefundUseCase;
    private final ConfirmWalletPaymentUseCaseImpl confirmWalletPaymentUseCase;
    private final ReleaseWalletPaymentUseCaseImpl releaseWalletPaymentUseCase;
    private final ReverseWalletPaymentUseCaseImpl reverseWalletPaymentUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(String payload, String signature) {
        // 1. Procesar webhook (verifica firma HMAC internamente)
        GatewayPaymentEvent event = paymentGatewayPort.processWebhook(payload, signature);

        // 2. Verificar idempotencia
        if (webhookRepository.existsByGatewayEventId(event.getEventId())) {
            log.info("Webhook already processed: eventId={}", event.getEventId());
            return;
        }

        // 3. Buscar pago por gatewayTransactionId
        Payment payment = paymentRepository.findByGatewayTransactionId(event.getPaymentId())
            .orElseThrow(() -> new PaymentNotFoundException(
                "Payment not found for gatewayTransactionId: " + event.getPaymentId()));

        // 4. Persistir TransactionWebhook
        TransactionWebhook webhook = TransactionWebhook.builder()
            .paymentId(payment.getPaymentId())
            .gatewayEventId(event.getEventId())
            .payload(payload)
            .receivedAt(LocalDateTime.now())
            .build();
        webhookRepository.save(webhook);

        // 5. Actualizar Payment según el evento
        applyGatewayStatus(payment, event.getStatus(), "Webhook: " + event.getEventType());
    }

    /**
     * Aplica el estado que reporta la pasarela sobre el pago y sus efectos en orden,
     * tickets y wallet. Lo usan el webhook y el job de conciliación, para que ambos
     * sigan las mismas transiciones. Es idempotente: un estado ya aplicado se ignora.
     */
    @Transactional
    public void applyGatewayStatus(Payment payment, String gatewayStatus, String reason) {
        PaymentStatus oldStatus = payment.getStatus();

        try {
            switch (gatewayStatus.toUpperCase()) {
                case "APPROVED":
                    // Idempotencia: si ya está APPROVED, ignorar el webhook duplicado
                    if (payment.getStatus() == PaymentStatus.APPROVED) {
                        log.info("Payment already APPROVED, skipping duplicate webhook: paymentId={}", payment.getPaymentId());
                        return;
                    }
                    payment.approve();
                    paymentRepository.save(payment);
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.APPROVED, reason);

                    // Confirmar orden y tickets
                    confirmOrderPaymentUseCase.confirmOrderPayment(
                        payment.getOrderId(), payment.getGatewayTransactionId());

                    // Si había reserva wallet, confirmarla (descuenta balance)
                    if (payment.getWalletTransactionId() != null) {
                        confirmWalletPaymentUseCase.execute(payment.getWalletTransactionId());
                        log.info("Wallet payment confirmed on APPROVED: txId={}", payment.getWalletTransactionId());
                    }

                    // Load order for notification event
                    Order approvedOrder = orderRepository.findById(payment.getOrderId()).orElse(null);

                    // Publish event for notifications
                    try {
                        if (approvedOrder != null) {
                            PaymentApprovedEvent approvedEvent = PaymentApprovedEvent.builder()
                                    .paymentId(payment.getPaymentId())
                                    .userId(approvedOrder.getUserId().intValue())
                                    .walletAmount(payment.getWalletAmountApplied())
                                    .gatewayAmount(payment.getAmount() != null ? payment.getAmount().getAmount() : BigDecimal.ZERO)
                                    .currency(payment.getAmount() != null ? payment.getAmount().getCurrency().toString() : "USD")
                                    .build();
                            eventPublisher.publishEvent(approvedEvent);
                            log.debug("PaymentApprovedEvent published: paymentId={}", payment.getPaymentId());
                        } else {
                            log.warn("Order not found for payment {}, PaymentApprovedEvent not published", payment.getPaymentId());
                        }
                    } catch (Exception e) {
                        log.error("Failed to publish PaymentApprovedEvent (non-breaking): paymentId={}", 
                                payment.getPaymentId(), e);
                    }

                    log.info("Payment approved: paymentId={}, orderId={}", payment.getPaymentId(), payment.getOrderId());
                    break;

                case "DECLINED":
                    if (payment.getStatus() == PaymentStatus.REJECTED) {
                        log.info("Payment already REJECTED, skipping duplicate webhook: paymentId={}", payment.getPaymentId());
                        return;
                    }
                    payment.reject();
                    paymentRepository.save(payment);
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.REJECTED, reason);

                    failOrderPaymentUseCase.failOrderPayment(payment.getOrderId(), "Payment declined");

                    // Liberar reserva wallet si existe
                    if (payment.getWalletTransactionId() != null) {
                        releaseWalletPaymentUseCase.execute(payment.getWalletTransactionId());
                        log.info("Wallet payment released on DECLINED: txId={}", payment.getWalletTransactionId());
                    }

                    log.info("Payment declined: paymentId={}, orderId={}", payment.getPaymentId(), payment.getOrderId());
                    break;

                case "FAILED":
                    if (payment.getStatus() == PaymentStatus.ERROR) {
                        log.info("Payment already in ERROR, skipping duplicate webhook: paymentId={}", payment.getPaymentId());
                        return;
                    }
                    payment.markAsError();
                    paymentRepository.save(payment);
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.ERROR, reason);

                    failOrderPaymentUseCase.failOrderPayment(payment.getOrderId(), "Payment failed");

                    if (payment.getWalletTransactionId() != null) {
                        releaseWalletPaymentUseCase.execute(payment.getWalletTransactionId());
                        log.info("Wallet payment released on FAILED: txId={}", payment.getWalletTransactionId());
                    }

                    log.info("Payment failed: paymentId={}, orderId={}", payment.getPaymentId(), payment.getOrderId());
                    break;

                case "CANCELLED":
                    if (payment.getStatus() == PaymentStatus.ERROR) {
                        log.info("Payment already in ERROR (cancelled), skipping duplicate webhook: paymentId={}", payment.getPaymentId());
                        return;
                    }
                    payment.markAsError();
                    paymentRepository.save(payment);
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.ERROR,
                        reason + " (payment cancelled by user)");

                    failOrderPaymentUseCase.failOrderPayment(payment.getOrderId(), "Payment cancelled");

                    if (payment.getWalletTransactionId() != null) {
                        releaseWalletPaymentUseCase.execute(payment.getWalletTransactionId());
                        log.info("Wallet payment released on CANCELLED: txId={}", payment.getWalletTransactionId());
                    }

                    log.info("Payment cancelled: paymentId={}, orderId={}", payment.getPaymentId(), payment.getOrderId());
                    break;

                case "REFUNDED":
                    if (payment.getStatus() == PaymentStatus.REFUNDED) {
                        log.info("Payment already REFUNDED, skipping duplicate webhook: paymentId={}", payment.getPaymentId());
                        return;
                    }
                    payment.refund();
                    paymentRepository.save(payment);
                    savePaymentLog(payment.getPaymentId(), oldStatus, PaymentStatus.REFUNDED, reason);

                    // Actualizar Refund a PROCESSED
                    refundRepository.findByPaymentId(payment.getPaymentId()).ifPresent(refund -> {
                        Refund processed = Refund.builder()
                            .refundId(refund.getRefundId())
                            .paymentId(refund.getPaymentId())
                            .status(RefundStatus.PROCESSED)
                            .reason(refund.getReason())
                            .requestedAt(refund.getRequestedAt())
                            .processedAt(LocalDateTime.now())
                            .build();
                        refundRepository.save(processed);
                    });

                    // Transicionar Order y Tickets a REFUNDED (los tickets USED se conservan)
                    var refundedTickets = confirmRefundUseCase.confirmRefund(payment.getOrderId());
                    if (refundedTickets.isPartial()) {
                        // La pasarela ya devolvió el total: algún ticket se usó mientras el reembolso estaba pendiente
                        log.warn("Gateway refund completed but {} of {} ticket(s) were used while it was pending: paymentId={}, orderId={}",
                            refundedTickets.totalCount() - refundedTickets.refundableCount(), refundedTickets.totalCount(),
                            payment.getPaymentId(), payment.getOrderId());
                    }

                    // Si había crédito wallet aplicado, devolver el saldo
                    BigDecimal walletApplied = payment.getWalletAmountApplied();
                    if (walletApplied != null && walletApplied.compareTo(BigDecimal.ZERO) > 0
                            && payment.getWalletTransactionId() != null) {
                        reverseWalletPaymentUseCase.execute(
                            payment.getWalletTransactionId(),
                            "Refund: wallet amount returned for payment " + payment.getPaymentId());
                        log.info("Wallet payment reversed on REFUNDED: txId={}, amount={}",
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
                            log.debug("PaymentRefundedEvent published: paymentId={}", payment.getPaymentId());
                        } else {
                            log.warn("Order not found for payment {}, PaymentRefundedEvent not published", payment.getPaymentId());
                        }
                    } catch (Exception e) {
                        log.error("Failed to publish PaymentRefundedEvent (non-breaking): paymentId={}", 
                                payment.getPaymentId(), e);
                    }

                    log.info("Payment refunded: paymentId={}, orderId={}", payment.getPaymentId(), payment.getOrderId());
                    break;

                default:
                    log.warn("Unhandled payment status from gateway: status={}, paymentId={}",
                        gatewayStatus, payment.getPaymentId());
            }
        } catch (InvalidPaymentStatusTransitionException e) {
            log.error("Invalid payment status transition: paymentId={}, from={}, to={}, reason={}",
                payment.getPaymentId(), oldStatus, gatewayStatus, e.getMessage());
            throw e;
        }
    }

    private void savePaymentLog(Long paymentId, PaymentStatus oldStatus,
                                PaymentStatus newStatus, String reason) {
        PaymentLog paymentLog = PaymentLog.builder()
            .paymentId(paymentId)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .changedAt(LocalDateTime.now())
            .reason(reason)
            .build();
        paymentLogRepository.save(paymentLog);
    }
}
