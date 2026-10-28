package com.capysoft.tuevento.shared.infrastructure.notification;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * notification-service crea las notificaciones y pide su traducción por
 * {@link NotificationTopics#TRANSLATION}; aquí se encola en el sistema de traducciones del
 * monolito, que las escribe en notification_translation (misma base de datos).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTranslationRequestListener {

    private static final String ENTITY_TYPE = "notification";

    private final TranslationRequests translationRequests;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = NotificationTopics.TRANSLATION, groupId = "monolito-translation")
    public void onTranslationRequested(String rawMessage) {
        try {
            JsonNode message = objectMapper.readTree(rawMessage);
            if (!message.hasNonNull("notificationId")) {
                log.warn("Translation request discarded: no notificationId");
                return;
            }
            translationRequests.request(ENTITY_TYPE, message.get("notificationId").asLong());
        } catch (Exception e) {
            // Sin traducción la bandeja muestra el texto en español, igual que hoy si falla
            log.error("Could not request notification translation", e);
        }
    }
}
