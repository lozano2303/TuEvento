package com.capysoft.tuevento.modules.notification.infrastructure.remote;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Modo {@code remote}: el microservicio crea las notificaciones y pide su traducción por
 * {@link NotificationTopics#TRANSLATION}; aquí se encola en el sistema de traducciones del
 * monolito, que las escribe en notification_translation (misma base de datos).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.mode", havingValue = "remote")
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
