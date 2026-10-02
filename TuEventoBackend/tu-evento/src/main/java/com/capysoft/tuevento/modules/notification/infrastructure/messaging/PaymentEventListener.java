package com.capysoft.tuevento.modules.notification.infrastructure.messaging;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationEntityTypes;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import com.capysoft.tuevento.modules.payment.domain.event.PaymentApprovedEvent;
import com.capysoft.tuevento.modules.payment.domain.event.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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

    private final SendNotificationUseCase sendNotificationUseCase;

    public PaymentEventListener(SendNotificationUseCase sendNotificationUseCase) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        log.info("=== DEBUG: PaymentEventListener INITIALIZED ===");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentApproved(PaymentApprovedEvent event) {
        try {
            log.info("=== DEBUG: PaymentEventListener.handlePaymentApproved called ===");
            log.info("Event: paymentId={}, userId={}, walletAmount={}, gatewayAmount={}", 
                    event.getPaymentId(), event.getUserId(), event.getWalletAmount(), event.getGatewayAmount());
                    
            log.debug("Processing PaymentApprovedEvent: paymentId={}, userId={}", 
                    event.getPaymentId(), event.getUserId());

            SendNotificationCommand command = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.PAYMENT_APPROVED)
                    .entityType(NotificationEntityTypes.PAYMENT)
                    .entityId(event.getPaymentId())
                    .userIds(List.of(event.getUserId()))
                    .walletAmount(event.getWalletAmount())
                    .gatewayAmount(event.getGatewayAmount())
                    .currency(event.getCurrency())
                    .build();

            sendNotificationUseCase.execute(command);
            
            log.info("PaymentApprovedEvent processed successfully: paymentId={}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to process PaymentApprovedEvent: paymentId={}", event.getPaymentId(), e);
            // No re-lanzar la excepción para evitar afectar el flujo original
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        try {
            log.debug("Processing PaymentRefundedEvent: paymentId={}, userId={}", 
                    event.getPaymentId(), event.getUserId());

            SendNotificationCommand command = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.PAYMENT_REFUNDED)
                    .entityType(NotificationEntityTypes.PAYMENT)
                    .entityId(event.getPaymentId())
                    .userIds(List.of(event.getUserId()))
                    .walletAmount(event.getWalletAmount())
                    .gatewayAmount(event.getGatewayAmount())
                    .currency(event.getCurrency())
                    .build();

            sendNotificationUseCase.execute(command);
            
            log.info("PaymentRefundedEvent processed successfully: paymentId={}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to process PaymentRefundedEvent: paymentId={}", event.getPaymentId(), e);
            // No re-lanzar la excepción para evitar afectar el flujo original
        }
    }
}