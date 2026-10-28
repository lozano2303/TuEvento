package com.capysoft.tuevento.shared.infrastructure.websocket;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * El /ws sigue en el monolito (web y móvil no cambian), así que los avisos en vivo que genera
 * identity-service llegan por {@link #TOPIC} y se reenvían aquí al canal de sesión del usuario.
 *
 * <p>Solo se acepta {@code /topic/users/{userId}/session} con los tipos de {@link #ALLOWED_TYPES}, y
 * el payload se arma aquí ({@code {type}}): por ese canal también viajan ROLE_REVOKED o
 * ROLE_UPDATED, que la web usa para cerrar o renovar la sesión, y un mensaje de Kafka no debe
 * poder provocarlos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class IdentityPushRelay {

    public static final String TOPIC = "tuevento.identidad.push";
    static final Pattern ALLOWED_DESTINATION = Pattern.compile("^/topic/users/\\d+/session$");
    static final Set<String> ALLOWED_TYPES = Set.of("THEME_UPDATED");

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PushMessage(String destination, Body body) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Body(String type) {
        }
    }

    @KafkaListener(topics = TOPIC, groupId = "monolito-identity-push")
    public void relay(String rawMessage) {
        try {
            PushMessage message = objectMapper.readValue(rawMessage, PushMessage.class);
            String type = message.body() == null ? null : message.body().type();
            if (message.destination() == null || !ALLOWED_DESTINATION.matcher(message.destination()).matches()
                    || !ALLOWED_TYPES.contains(type)) {
                log.warn("Identity push discarded: destination or type not allowed ({}, {})",
                        message.destination(), type);
                return;
            }
            messagingTemplate.convertAndSend(message.destination(), Map.of("type", type));
        } catch (Exception e) {
            // Un aviso perdido solo retrasa el refresco: el cliente lee el estado igual al recargar
            log.error("Could not relay identity push", e);
        }
    }
}
