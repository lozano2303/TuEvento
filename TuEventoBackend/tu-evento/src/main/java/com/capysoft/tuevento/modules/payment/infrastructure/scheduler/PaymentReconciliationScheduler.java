package com.capysoft.tuevento.modules.payment.infrastructure.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.payment.application.dto.GatewayPayment;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.application.usecase.ProcessWebhookUseCaseImpl;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Job de conciliación de pagos PENDING.
 *
 * <p>Si el webhook de la pasarela se pierde, el pago queda en PENDING y la orden en
 * PAYMENT_PENDING para siempre. Este job busca los pagos PENDING con más de
 * {@code payment.reconciliation.pending-threshold-minutes} minutos, consulta su estado
 * real en la pasarela y, si ya es terminal, aplica la misma transición que aplicaría el
 * webhook ({@link ProcessWebhookUseCaseImpl#applyGatewayStatus}): confirmar la orden y
 * los tickets, liberar o confirmar la reserva de wallet, o fallar la orden.
 *
 * <p>Si la pasarela sigue en PENDING pasados {@code payment.reconciliation.expire-after-minutes}
 * (el comprador abandonó el pago), el pago se cancela en la pasarela y se aplica CANCELLED:
 * la orden falla y libera sus asientos. Sin esto, las reservas con pago pendiente, que el
 * scheduler de asientos ya no libera, quedarían bloqueadas para siempre.
 *
 * <p>La consulta HTTP se hace sin transacción abierta; cada pago se aplica en su propia
 * transacción (bean colaborador), así un fallo en uno no afecta a los demás.
 */
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final ProcessWebhookUseCaseImpl processWebhookUseCase;

    @Value("${payment.reconciliation.pending-threshold-minutes:15}")
    private int pendingThresholdMinutes;

    @Value("${payment.reconciliation.expire-after-minutes:30}")
    private int expireAfterMinutes;

    @Value("${payment.reconciliation.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${payment.reconciliation.cron:0 */5 * * * *}")
    public void reconcilePendingPayments() {
        if (!enabled) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(pendingThresholdMinutes);
        List<Payment> stale = paymentRepository.findPendingOlderThan(threshold);
        if (stale.isEmpty()) {
            log.debug("[Reconciliation] No stale PENDING payments (threshold={}min)", pendingThresholdMinutes);
            return;
        }

        log.info("[Reconciliation] {} stale PENDING payment(s) older than {}min", stale.size(), pendingThresholdMinutes);
        stale.forEach(this::reconcileOne);
    }

    /** Consulta la pasarela y aplica el estado si es terminal. Los errores se registran y se reintenta en el próximo ciclo. */
    void reconcileOne(Payment payment) {
        if (payment.getGatewayTransactionId() == null) {
            log.debug("[Reconciliation] Skipping payment {}: no gatewayTransactionId", payment.getPaymentId());
            return;
        }

        try {
            GatewayPayment gatewayPayment = paymentGatewayPort.getPayment(payment.getGatewayTransactionId());
            String gatewayStatus = gatewayPayment.getStatus().toUpperCase();

            if ("PENDING".equals(gatewayStatus)) {
                if (!isExpired(payment)) {
                    log.info("[Reconciliation] Payment {} still PENDING in gateway", payment.getPaymentId());
                    return;
                }
                // Si la pasarela ya lo aprobó, cancelPayment falla y el próximo ciclo verá APPROVED
                paymentGatewayPort.cancelPayment(payment.getGatewayTransactionId());
                gatewayStatus = "CANCELLED";
                log.info("[Reconciliation] Payment {} expired after {}min; cancelled in gateway",
                    payment.getPaymentId(), expireAfterMinutes);
            }

            processWebhookUseCase.applyGatewayStatus(payment, gatewayStatus,
                "Reconciliation: gateway status=" + gatewayStatus);
            log.info("[Reconciliation] Payment {} reconciled with gateway status {}", payment.getPaymentId(), gatewayStatus);
        } catch (Exception e) {
            log.error("[Reconciliation] Could not reconcile paymentId={}, gatewayTxId={}; will retry",
                payment.getPaymentId(), payment.getGatewayTransactionId(), e);
        }
    }

    private boolean isExpired(Payment payment) {
        return payment.getCreatedAt() != null
            && payment.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(expireAfterMinutes));
    }
}
