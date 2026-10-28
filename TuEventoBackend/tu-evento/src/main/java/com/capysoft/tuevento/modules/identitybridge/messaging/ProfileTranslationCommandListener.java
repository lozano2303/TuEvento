package com.capysoft.tuevento.modules.identitybridge.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * identity-service atiende los perfiles, pero el motor de traducciones sigue aquí: pide traducir la
 * biografía ({@code REQUEST}) o descarta los trabajos del texto anterior cuando cambia
 * ({@code DISCARD}) por {@link #TOPIC}, con el profileId como clave para conservar el orden. El
 * motor escribe el resultado en profile_translation (misma base de datos).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class ProfileTranslationCommandListener {

    public static final String TOPIC = "tuevento.identidad.translation";
    static final String ENTITY_TYPE = "profile";

    private final TranslationRequests translationRequests;
    private final ObjectMapper objectMapper;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Command(String action, Long profileId, String sourceLanguage) {
    }

    @KafkaListener(topics = TOPIC, groupId = "monolito-identity-translation")
    public void onCommand(String rawMessage) {
        try {
            Command command = objectMapper.readValue(rawMessage, Command.class);
            if (command.profileId() == null || command.action() == null) {
                log.warn("Profile translation command discarded: action and profileId are required");
                return;
            }
            switch (command.action()) {
                // El idioma origen es siempre el español (lo fija el núcleo de traducción)
                case "REQUEST" -> translationRequests.request(ENTITY_TYPE, command.profileId());
                case "DISCARD" -> translationRequests.discard(ENTITY_TYPE, command.profileId());
                default -> log.warn("Profile translation command discarded: unknown action {}", command.action());
            }
        } catch (Exception e) {
            // Sin traducción el perfil se muestra en español; identity-service la vuelve a pedir al leerlo
            log.error("Could not apply profile translation command", e);
        }
    }
}
