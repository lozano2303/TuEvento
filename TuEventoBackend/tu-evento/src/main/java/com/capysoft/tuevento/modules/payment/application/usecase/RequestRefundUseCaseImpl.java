package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.domain.model.*;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentLogRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.RefundRepository;
import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmRefundUseCase;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReverseWalletPaymentUseCaseImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * El cierre local está extraído a closeRefundLocally() y es compartido
 * con el caso webhook de ProcessWebhookUseCaseImpl para evitar duplicación.
 */
@Service
@RequiredArgsConstructor
public class RequestRefundUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(RequestRefundUseCaseImpl.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final ConfirmRefundUseCase confirmRefundUseCase;
    private final ReverseWalletPaymentUseCaseImpl reverseWalletPaymentUseCase;

    @Transactional
    public void execute(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new IllegalStateException(
                String.format("Cannot refund payment %d: current status is %s, expected APPROVED",
                    paymentId, payment.getStatus()));
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
            closeRefundLocally(payment, savedRefund, reason);
        } else {
            // Pago con pasarela — el gateway dispara el webhook payment.refunded
            paymentGatewayPort.refundPayment(payment.getGatewayTransactionId());
        }
    }

    /**
     * Cierre local del reembolso (para pagos wallet-only).
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
            .reason("Wallet-only refund: " + reason)
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
                "Wallet-only refund: " + reason);
            log.info("Wallet credit reversed on wallet-only refund: txId={}, amount={}",
                payment.getWalletTransactionId(), walletApplied);
        }

        log.info("Wallet-only refund closed locally: paymentId={}, orderId={}",
            payment.getPaymentId(), payment.getOrderId());
    }
}
