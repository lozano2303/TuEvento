package com.capysoft.tuevento.modules.notification.infrastructure.channel;

import com.capysoft.tuevento.modules.notification.application.port.out.NotificationChannelPort;
import com.capysoft.tuevento.modules.notification.domain.model.DeliveredStatus;
import com.capysoft.tuevento.modules.notification.domain.model.Notification;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationUser;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Canal IN_APP: La fila notification_user nace en DELIVERED (guardarla ya es entregarla).
 * Además intenta empujar la notificación en vivo por WebSocket STOMP al destino
 * /user/queue/notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InAppChannelAdapter implements NotificationChannelPort {

    private final NotificationUserRepository notificationUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public String channelName() {
        return NotificationChannelNames.IN_APP;
    }

    @Override
    public void deliver(Notification notification, List<Integer> userIds) {
        for (Integer userId : userIds) {
            try {
                // Guardar como DELIVERED (el guardado ya es la entrega)
                NotificationUser saved = notificationUserRepository.save(NotificationUser.builder()
                        .notificationId(notification.getNotificationId())
                        .userId(userId)
                        .emailAddress(null)  // IN_APP no usa email
                        .deliveredStatus(DeliveredStatus.DELIVERED)
                        .errorMessage(null)
                        .build());

                log.debug("IN_APP notification saved: notificationUserId={}, userId={}",
                        saved.getNotificationUserId(), userId);

                // Intentar push por WebSocket (nunca debe romper el guardado)
                try {
                    Map<String, Object> message = Map.of(
                            "notificationUserId", saved.getNotificationUserId(),
                            "type", "notification",
                            "subject", notification.getSubject(),
                            "body", notification.getBody(),
                            "entityType", notification.getEntityType(),
                            "entityId", notification.getEntityId(),
                            "sentAt", notification.getSentAt() != null ? notification.getSentAt().toString() : null
                    );

                    messagingTemplate.convertAndSendToUser(
                            userId.toString(),
                            "/queue/notifications",
                            message
                    );
                    
                    log.debug("WebSocket push attempted for userId={}", userId);
                } catch (Exception wsError) {
                    log.warn("WebSocket push failed (non-breaking): userId={}, error={}",
                            userId, wsError.getMessage());
                    // No se actualiza el estado DELIVERED porque el push es solo una mejora UX
                }

            } catch (Exception e) {
                log.error("Failed to deliver IN_APP notification: userId={}, notificationId={}",
                        userId, notification.getNotificationId(), e);
                // Intentar guardar como FAILED
                try {
                    notificationUserRepository.save(NotificationUser.builder()
                            .notificationId(notification.getNotificationId())
                            .userId(userId)
                            .emailAddress(null)
                            .deliveredStatus(DeliveredStatus.FAILED)
                            .errorMessage(truncate(e.getMessage(), 1000))
                            .build());
                } catch (Exception saveError) {
                    log.error("Failed to save FAILED IN_APP notification: userId={}", userId, saveError);
                }
            }
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}