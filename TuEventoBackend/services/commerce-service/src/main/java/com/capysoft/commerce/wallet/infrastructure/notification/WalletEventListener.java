package com.capysoft.commerce.wallet.infrastructure.notification;

import com.capysoft.commerce.shared.notification.SendNotificationCommand;
import com.capysoft.commerce.shared.notification.NotificationSender;
import com.capysoft.commerce.shared.notification.NotificationEntityTypes;
import com.capysoft.commerce.shared.notification.NotificationTypeNames;
import com.capysoft.commerce.wallet.domain.event.WalletCreditedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Listener de eventos de wallet.
 * Usa @TransactionalEventListener(phase = AFTER_COMMIT) + @Async para desacoplar.
 * Como AFTER_COMMIT corre fuera de la transacción original,
 * necesita @Transactional(propagation = REQUIRES_NEW).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventListener {

    private final NotificationSender notificationSender;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleWalletCredited(WalletCreditedEvent event) {
        try {
            log.debug("Processing WalletCreditedEvent: walletTransactionId={}, userId={}", 
                    event.getWalletTransactionId(), event.getUserId());

            SendNotificationCommand command = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.WALLET_CREDITED)
                    .entityType(NotificationEntityTypes.WALLET_TRANSACTION)
                    .entityId(event.getWalletTransactionId())
                    .userIds(List.of(event.getUserId()))
                    .creditedAmount(event.getAmount())
                    .currency(event.getCurrency())
                    .reason(event.getReason())
                    .build();

            notificationSender.send(command);
            
            log.info("WalletCreditedEvent processed successfully: walletTransactionId={}", 
                    event.getWalletTransactionId());
        } catch (Exception e) {
            log.error("Failed to process WalletCreditedEvent: walletTransactionId={}", 
                    event.getWalletTransactionId(), e);
            // No re-lanzar la excepción para evitar afectar el flujo original
        }
    }
}