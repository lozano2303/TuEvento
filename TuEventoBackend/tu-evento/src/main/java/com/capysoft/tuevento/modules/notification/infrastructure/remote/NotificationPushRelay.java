package com.capysoft.tuevento.modules.notification.infrastructure.remote;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Modo {@code remote}: el /ws sigue en el monolito (web y móvil no cambian), así que los avisos
 * en tiempo real que genera el microservicio llegan por {@link NotificationTopics#PUSH} y se
 * reenvían aquí. Solo se aceptan los destinos de notificaciones de un usuario: un mensaje no
 * puede publicar en otros canales del broker (asientos, etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.mode", havingValue = "remote")
public class NotificationPushRelay {

    static final Pattern ALLOWED_DESTINATION = Pattern.compile("^/topic/notifications/\\d+$");

    private static final TypeReference<Map<String, Object>> MESSAGE = new TypeReference<>() {
    };

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = NotificationTopics.PUSH, groupId = "monolito-push")
    public void relay(String rawMessage) {
        try {
            Map<String, Object> message = objectMapper.readValue(rawMessage, MESSAGE);
            Object destination = message.get("destination");
            if (!(destination instanceof String target) || !ALLOWED_DESTINATION.matcher(target).matches()) {
                log.warn("Push discarded: destination not allowed ({})", destination);
                return;
            }
            messagingTemplate.convertAndSend(target, message.get("body"));
        } catch (Exception e) {
            // Un push perdido solo retrasa el aviso: la bandeja lo muestra igual al recargar
            log.error("Could not relay notification push", e);
        }
    }
}
