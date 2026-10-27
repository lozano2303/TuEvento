package com.capysoft.notification.infrastructure.messaging;

import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.capysoft.notification.application.port.out.NotificationEventsPort;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Publica en Kafka los pushes y las peticiones de traducción que procesa el monolito. Los
 * errores se registran y no se propagan: un push perdido solo retrasa el aviso (la bandeja lo
 * muestra igual) y sin traducción se sirve el texto en español.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationEventsPublisher implements NotificationEventsPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void push(Integer userId, Map<String, Object> body) {
        publish(NotificationTopics.PUSH, String.valueOf(userId),
                Map.of("destination", "/topic/notifications/" + userId, "body", body));
    }

    @Override
    public void requestTranslation(Long notificationId) {
        publish(NotificationTopics.TRANSLATION, String.valueOf(notificationId), Map.of("notificationId", notificationId));
    }

    private void publish(String topic, String key, Map<String, Object> message) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("Could not publish to {} (key={})", topic, key, e);
        }
    }
}
