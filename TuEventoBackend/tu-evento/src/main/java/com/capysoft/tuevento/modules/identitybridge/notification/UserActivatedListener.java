package com.capysoft.tuevento.modules.identitybridge.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.tuevento.shared.application.notification.SendNotificationCommand;
import com.capysoft.tuevento.shared.application.notification.NotificationSender;
import com.capysoft.tuevento.shared.application.notification.NotificationTypeNames;
import com.capysoft.tuevento.shared.application.notification.RecipientDirectoryPort;
import com.capysoft.tuevento.shared.domain.event.identity.UserActivatedEvent;

import lombok.RequiredArgsConstructor;

/**
 * Listens to UserActivatedEvent and sends a welcome notification after account activation.
 * Solo tras el commit de la activación: si la activación hace rollback no hay bienvenida.
 */
@Component
@RequiredArgsConstructor
public class UserActivatedListener {

    private static final Logger log = LoggerFactory.getLogger(UserActivatedListener.class);

    private final NotificationSender notificationSender;
    private final RecipientDirectoryPort recipientDirectoryPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserActivated(UserActivatedEvent event) {
        log.info("User activated event detected: userId={}, alias={}", event.getUserId(), event.getAlias());

        try {
            // Get user's full name from profile
            String fullName = recipientDirectoryPort.findRecipient(event.getUserId())
                    .map(RecipientDirectoryPort.Recipient::fullName)
                    .orElse(event.getAlias());

            // Send welcome notification via IN_APP and EMAIL
            SendNotificationCommand command = SendNotificationCommand.builder()
                    .userIds(List.of(event.getUserId()))
                    .typeName(NotificationTypeNames.WELCOME)
                    .entityType("USER")
                    .entityId(event.getUserId().longValue())
                    .reason(fullName) // Use reason field to pass the user's name
                    .build();

            notificationSender.send(command);
            log.info("Welcome notification sent to user: userId={}, name={}", event.getUserId(), fullName);

        } catch (Exception e) {
            log.error("Failed to send welcome notification to user: userId={}, error={}", 
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
