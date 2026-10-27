package com.capysoft.identity.shared.infrastructure.messaging;

import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.capysoft.identity.theme.infrastructure.messaging.ThemeUpdatedPushPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envía un aviso en vivo por {@link ThemeUpdatedPushPublisher#TOPIC}; el monolito lo reenvía al /ws
 * (solo destinos y tipos permitidos). Un aviso perdido solo retrasa el refresco del cliente, así
 * que un fallo de Kafka se registra y no rompe la operación que lo originó.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LivePushSender {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(String key, String destination, Map<String, Object> body) {
        try {
            String message = objectMapper.writeValueAsString(Map.of("destination", destination, "body", body));
            kafkaTemplate.send(ThemeUpdatedPushPublisher.TOPIC, key, message)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Live push to {} not sent: {}", destination, error.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Live push to {} not sent: {}", destination, e.getMessage());
        }
    }
}
