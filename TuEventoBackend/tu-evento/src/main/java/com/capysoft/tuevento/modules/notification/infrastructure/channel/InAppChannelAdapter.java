package com.capysoft.tuevento.modules.notification.infrastructure.channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.notification.application.port.out.NotificationChannelPort;
import com.capysoft.tuevento.modules.notification.domain.model.DeliveredStatus;
import com.capysoft.tuevento.modules.notification.domain.model.Notification;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationUser;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository.JpaNotificationTypeRepository jpaNotificationTypeRepository;

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

                // Obtener el nombre del tipo de notificación
                String typeName = jpaNotificationTypeRepository.findById(notification.getNotificationTypeId())
                        .map(entity -> entity.getName())
                        .orElse("UNKNOWN");

                // Intentar push por WebSocket (nunca debe romper el guardado)
                try {
                    // Usar HashMap en lugar de Map.of() porque Map.of() no permite valores null
                    Map<String, Object> message = new HashMap<>();
                    message.put("notificationUserId", saved.getNotificationUserId());
                    message.put("type", typeName);
                    message.put("subject", notification.getSubject());
                    message.put("body", notification.getBody());
                    message.put("entityType", notification.getEntityType() != null ? notification.getEntityType() : "");
                    message.put("entityId", notification.getEntityId() != null ? notification.getEntityId().longValue() : 0L);
                    message.put("sentAt", notification.getSentAt() != null ? notification.getSentAt().toString() : null);
                    message.put("readAt", saved.getReadAt() != null ? saved.getReadAt().toString() : null);
                    message.put("read", false);  // Nueva notificación siempre es no leída

                    // Usar topic en lugar de user destination para evitar problemas con el simple broker
                    // El frontend se suscribe a /topic/notifications/{userId}
                    String topicDestination = "/topic/notifications/" + userId;
                    
                    messagingTemplate.convertAndSend(topicDestination, message);
                    
                    log.debug("[InAppChannel] Message sent to {} for userId={}, type={}", 
                            topicDestination, userId, typeName);
                } catch (Exception wsError) {
                    log.error("[InAppChannel] WebSocket push failed (non-breaking): userId={}, error={}", 
                            userId, wsError.getMessage(), wsError);
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