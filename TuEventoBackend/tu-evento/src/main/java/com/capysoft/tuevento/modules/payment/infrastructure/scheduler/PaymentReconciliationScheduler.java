package com.capysoft.tuevento.modules.payment.infrastructure.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.payment.application.dto.GatewayPayment;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.application.usecase.ProcessWebhookUseCaseImpl;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentLog;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentLogRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.application.port.in.FailOrderPaymentUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Job de conciliación de pagos PENDING.
 *
 * <h3>Problema que resuelve</h3>
 * <p>Cuando la llamada HTTP al gateway retorna OK pero la persistencia del
 * {@code Payment} local falla (o el proceso muere antes de completarla),
 * el pago existe en el gateway sin registro local. El webhook nunca llega
 * porque no hay ningún {@code gatewayTransactionId} guardado.
 *
 * <p>Este scheduler detecta ese escenario consultando el gateway para cada
 * pago que lleve más de {@code payment.reconciliation.pending-threshold-minutes}
 * minutos en estado PENDING sin haber recibido un webhook.
 *
 * <h3>Flujo por pago</h3>
 * <ol>
 *   <li>Busca todos los {@code Payment} en PENDING creados hace más de N minutos.
 *   <li>Consulta el estado real en el gateway via {@code PaymentGatewayPort.getPayment()}.
 *   <li>Si el gateway retorna APPROVED/DECLINED/FAILED/CANCELLED/REFUNDED, delega
 *       a {@code ProcessWebhookUseCaseImpl} para aplicar la misma lógica que un webhook.
 *       Esto mantiene un único punto de verdad para las transiciones de pago.
 *   <li>Si el gateway retorna PENDING, el pago todavía está en vuelo — se deja para
 *       la siguiente ejecución del scheduler.
 *   <li>Si el gateway responde con error, se loguea y se deja para reintento.
 * </ol>
 *
 * <p>El uso de {@code ProcessWebhookUseCaseImpl} garantiza idempotencia: si entre
 * la consulta y el procesamiento llega el webhook real, la verificación de
 * {@code gatewayEventId} evita el doble procesamiento.
 *
 * <h3>Configuración</h3>
 * <pre>
 * payment.reconciliation.enabled=true
 * payment.reconciliation.pending-threshold-minutes=15
 * payment.reconciliation.cron=0 * /5 * * * *   # cada 5 minutos
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final FailOrderPaymentUseCase failOrderPaymentUseCase;

    @Value("${payment.reconciliation.pending-threshold-minutes:15}")
    private int pendingThresholdMinutes;

    @Value("${payment.reconciliation.enabled:true}")
    private boolean enabled;

    /**
     * Se ejecuta cada 5 minutos. El cron es configurable via
     * {@code payment.reconciliation.cron} si se necesita ajustar en prod.
     */
    @Scheduled(cron = "${payment.reconciliation.cron:0 */5 * * * *}")
    @Transactional
    public void reconcilePendingPayments() {
        if (!enabled) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(pendingThresholdMinutes);
        List<Payment> stale = paymentRepository.findPendingOlderThan(threshold);

        if (stale.isEmpty()) {
            log.debug("[Reconciliation] No stale PENDING payments found (threshold={}min).", pendingThresholdMinutes);
            return;
        }

        log.info("[Reconciliation] Found {} stale PENDING payment(s) older than {}min.",
            stale.size(), pendingThresholdMinutes);

        for (Payment payment : stale) {
            reconcileOne(payment);
        }
    }

    /**
     * Consulta el estado real del pago en el gateway y aplica la transición
     * correspondiente. Cada pago se procesa en su propia transacción para que
     * un fallo en uno no impida procesar los demás.
     */
    public void reconcileOne(Payment payment) {
        if (payment.getGatewayTransactionId() == null) {
            // Payment en ERROR guardado por handleGatewayFailure — no tiene gatewayTxId.
            // No hay nada que consultar en el gateway.
            log.debug("[Reconciliation] Skipping payment {} — no gatewayTransactionId.", payment.getPaymentId());
            return;
        }

        log.info("[Reconciliation] Querying gateway for paymentId={}, gatewayTxId={}",
            payment.getPaymentId(), payment.getGatewayTransactionId());

        try {
            GatewayPayment gatewayPayment = paymentGatewayPort.getPayment(
                payment.getGatewayTransactionId());

            String gatewayStatus = gatewayPayment.getStatus().toUpperCase();

            switch (gatewayStatus) {
                case "PENDING" -> log.info(
                    "[Reconciliation] Payment {} still PENDING in gateway — will retry.", payment.getPaymentId());

                case "APPROVED", "DECLINED", "FAILED", "CANCELLED", "REFUNDED" -> {
                    log.info("[Reconciliation] Payment {} has terminal status {} in gateway — marking as reconciled.",
                        payment.getPaymentId(), gatewayStatus);
                    applyReconciliationTransition(payment, gatewayStatus);
                }

                default -> log.warn("[Reconciliation] Unhandled gateway status '{}' for paymentId={}",
                    gatewayStatus, payment.getPaymentId());
            }

        } catch (Exception e) {
            log.error("[Reconciliation] Failed to query gateway for paymentId={}, gatewayTxId={} — will retry next cycle.",
                payment.getPaymentId(), payment.getGatewayTransactionId(), e);
        }
    }

    /**
     * Aplica directamente la transición de estado sobre el payment sin pasar por
     * el flujo de webhook (que requiere un payload firmado). Registra un PaymentLog
     * de auditoría con origen "reconciliation" para distinguirlo de los webhooks normales.
     *
     * <p>Para los estados que afectan a la orden (DECLINED, FAILED, CANCELLED) también
     * falla la orden via {@code FailOrderPaymentUseCase} para liberar sillas.
     */
    private void applyReconciliationTransition(Payment payment, String gatewayStatus) {
        PaymentStatus oldStatus = payment.getStatus();
        PaymentStatus newStatus;
        String reason = "Reconciliation: gateway status=" + gatewayStatus;

        try {
            switch (gatewayStatus) {
                case "APPROVED"  -> { payment.approve();      newStatus = PaymentStatus.APPROVED; }
                case "DECLINED"  -> { payment.reject();       newStatus = PaymentStatus.REJECTED; }
                case "FAILED",
                     "CANCELLED" -> { payment.markAsError();  newStatus = PaymentStatus.ERROR; }
                case "REFUNDED"  -> { payment.refund();       newStatus = PaymentStatus.REFUNDED; }
                default          -> { return; }
            }

            paymentRepository.save(payment);

            paymentLogRepository.save(PaymentLog.builder()
                .paymentId(payment.getPaymentId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .reason(reason)
                .build());

            // Liberar sillas si el pago terminó en error/rechazo
            if (newStatus == PaymentStatus.REJECTED || newStatus == PaymentStatus.ERROR) {
                try {
                    failOrderPaymentUseCase.failOrderPayment(payment.getOrderId(), reason);
                    log.info("[Reconciliation] Order failed and seats released: orderId={}", payment.getOrderId());
                } catch (Exception failEx) {
                    log.error("[Reconciliation] Could not fail order {}: {}", payment.getOrderId(), failEx.getMessage());
                }
            }

            log.info("[Reconciliation] Payment {} transitioned {} → {}",
                payment.getPaymentId(), oldStatus, newStatus);

        } catch (Exception e) {
            log.error("[Reconciliation] Error applying transition for paymentId={}: {}",
                payment.getPaymentId(), e.getMessage(), e);
        }
    }
}
