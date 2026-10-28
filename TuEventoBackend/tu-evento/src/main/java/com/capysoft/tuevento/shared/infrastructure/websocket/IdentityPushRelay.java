package com.capysoft.tuevento.shared.infrastructure.websocket;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
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
 * el payload se arma aquí con sus campos conocidos ({@code {type}}, más {@code languageId} en
 * LANGUAGE_UPDATED y {@code role} en ROLE_UPDATED). ROLE_UPDATED solo con el rol ORGANIZER (el que
 * concede Identidad al aprobar una solicitud): ROLE_REVOKED, que cierra la sesión en la web, no se
 * acepta por aquí (lo envía Notificaciones con el evento de dominio).
 *
 * <p>Para {@code /topic/admin/pending-counts} no se reenvía nada del mensaje: se publica
 * {@link AdminPendingCountsChangedEvent} y el monolito envía los contadores que él calcula.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class IdentityPushRelay {

    public static final String TOPIC = "tuevento.identidad.push";
    static final Pattern ALLOWED_DESTINATION = Pattern.compile("^/topic/users/\\d+/session$");
    static final String THEME_UPDATED = "THEME_UPDATED";
    static final String LANGUAGE_UPDATED = "LANGUAGE_UPDATED";
    static final String ROLE_UPDATED = "ROLE_UPDATED";
    static final String GRANTED_ROLE = "ORGANIZER";
    static final Set<String> ALLOWED_TYPES = Set.of(THEME_UPDATED, LANGUAGE_UPDATED, ROLE_UPDATED);
    static final String ADMIN_PENDING_COUNTS = "/topic/admin/pending-counts";
    static final String PENDING_COUNTS_CHANGED = "PENDING_COUNTS_CHANGED";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PushMessage(String destination, Body body) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Body(String type, Long languageId, String role) {
        }
    }

    @KafkaListener(topics = TOPIC, groupId = "monolito-identity-push")
    public void relay(String rawMessage) {
        try {
            PushMessage message = objectMapper.readValue(rawMessage, PushMessage.class);
            String type = message.body() == null ? null : message.body().type();
            if (ADMIN_PENDING_COUNTS.equals(message.destination()) && PENDING_COUNTS_CHANGED.equals(type)) {
                eventPublisher.publishEvent(new AdminPendingCountsChangedEvent());
                return;
            }
            if (message.destination() == null || !ALLOWED_DESTINATION.matcher(message.destination()).matches()
                    || !ALLOWED_TYPES.contains(type)) {
                log.warn("Identity push discarded: destination or type not allowed ({}, {})",
                        message.destination(), type);
                return;
            }
            if (ROLE_UPDATED.equals(type)) {
                if (!GRANTED_ROLE.equals(message.body().role())) {
                    log.warn("Identity push discarded: ROLE_UPDATED with role {}", message.body().role());
                    return;
                }
                messagingTemplate.convertAndSend(message.destination(), Map.of("type", type, "role", GRANTED_ROLE));
                return;
            }
            if (LANGUAGE_UPDATED.equals(type)) {
                if (message.body().languageId() == null) {
                    log.warn("Identity push discarded: LANGUAGE_UPDATED without languageId");
                    return;
                }
                messagingTemplate.convertAndSend(message.destination(),
                        Map.of("type", type, "languageId", message.body().languageId()));
                return;
            }
            messagingTemplate.convertAndSend(message.destination(), Map.of("type", type));
        } catch (Exception e) {
            // Un aviso perdido solo retrasa el refresco: el cliente lee el estado igual al recargar
            log.error("Could not relay identity push", e);
        }
    }
}
