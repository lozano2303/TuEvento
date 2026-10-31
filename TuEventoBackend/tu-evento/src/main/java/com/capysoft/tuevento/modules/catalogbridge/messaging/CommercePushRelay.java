package com.capysoft.tuevento.modules.catalogbridge.messaging;

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
 * El /ws sigue en el monolito, así que el aviso de QR validado que genera commerce-service llega por
 * Kafka y se reenvía aquí. Solo se acepta el canal de entradas de un usuario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class CommercePushRelay {

    static final String TOPIC = "tuevento.comercio.push";
    static final Pattern ALLOWED_DESTINATION = Pattern.compile("^/topic/users/\\d+/tickets$");

    private static final TypeReference<Map<String, Object>> MESSAGE = new TypeReference<>() {
    };

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = "monolito-commerce-push")
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
            // El check-in ya quedó guardado; la app lo detecta igual consultando el ticket
            log.error("Could not relay commerce push", e);
        }
    }
}
