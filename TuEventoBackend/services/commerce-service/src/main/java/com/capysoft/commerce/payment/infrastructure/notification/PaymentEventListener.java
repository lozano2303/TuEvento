package com.capysoft.commerce.payment.infrastructure.notification;

import com.capysoft.commerce.shared.notification.SendNotificationCommand;
import com.capysoft.commerce.shared.notification.NotificationSender;
import com.capysoft.commerce.shared.notification.NotificationEntityTypes;
import com.capysoft.commerce.shared.notification.NotificationTypeNames;
import com.capysoft.commerce.payment.domain.event.PaymentApprovedEvent;
import com.capysoft.commerce.payment.domain.event.PaymentRefundedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;

/**
 * Listener de eventos de payment.
 * Usa @TransactionalEventListener(phase = AFTER_COMMIT) + @Async para desacoplar.
 * Como AFTER_COMMIT corre fuera de la transacción original,
 * necesita @Transactional(propagation = REQUIRES_NEW).
 */
@Slf4j
@Component
public class PaymentEventListener {

    private final NotificationSender notificationSender;

    public PaymentEventListener(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        notifyBuyer(NotificationTypeNames.PAYMENT_APPROVED, event.getPaymentId(), event.getUserId(),
                event.getWalletAmount(), event.getGatewayAmount(), event.getCurrency());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        notifyBuyer(NotificationTypeNames.PAYMENT_REFUNDED, event.getPaymentId(), event.getUserId(),
                event.getWalletAmount(), event.getGatewayAmount(), event.getCurrency());
    }

    /** Avisa al comprador con los importes del pago; un fallo no afecta al flujo del pago. */
    private void notifyBuyer(String typeName, Long paymentId, Integer userId,
                             BigDecimal walletAmount, BigDecimal gatewayAmount, String currency) {
        try {
            log.debug("Processing {}: paymentId={}, userId={}", typeName, paymentId, userId);

            notificationSender.send(SendNotificationCommand.builder()
                    .typeName(typeName)
                    .entityType(NotificationEntityTypes.PAYMENT)
                    .entityId(paymentId)
                    .userIds(List.of(userId))
                    .walletAmount(walletAmount)
                    .gatewayAmount(gatewayAmount)
                    .currency(currency)
                    .build());

            log.info("{} processed successfully: paymentId={}", typeName, paymentId);
        } catch (Exception e) {
            log.error("Failed to process {}: paymentId={}", typeName, paymentId, e);
        }
    }
}
