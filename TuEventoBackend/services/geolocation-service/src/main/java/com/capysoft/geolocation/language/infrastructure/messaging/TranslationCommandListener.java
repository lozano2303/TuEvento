package com.capysoft.geolocation.language.infrastructure.messaging;

import com.capysoft.geolocation.language.application.service.TranslationService;
import com.capysoft.geolocation.language.domain.repository.TranslationJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Atiende las órdenes del monolito. Repetir una petición es seguro: si el texto no cambió, los
 * trabajos se quedan como están. Un mensaje que no se puede leer se registra y se descarta, sin
 * bloquear la partición.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "translation.enabled", havingValue = "true")
public class TranslationCommandListener {

    private final TranslationService translationService;
    private final TranslationJobRepository translationJobRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TranslationTopics.COMMANDS, groupId = "localizacion-translation")
    public void onCommand(String rawMessage) {
        TranslationCommandMessage message;
        try {
            message = objectMapper.readValue(rawMessage, TranslationCommandMessage.class);
        } catch (Exception e) {
            log.error("Discarding unreadable translation command", e);
            return;
        }
        if (message.entityType() == null || message.entityId() == null) {
            log.warn("Discarding translation command without entity: action={}", message.action());
            return;
        }
        switch (String.valueOf(message.action())) {
            case TranslationCommandMessage.REQUEST -> {
                if (message.texts() == null || message.sourceLanguage() == null) {
                    log.warn("Discarding translation request without texts for {}/{}", message.entityType(), message.entityId());
                    return;
                }
                translationService.requestTranslation(message.entityType(), message.entityId(),
                        message.sourceLanguage(), message.texts());
            }
            case TranslationCommandMessage.DISCARD -> {
                int deleted = translationJobRepository.deleteByEntity(message.entityType(),
                        Math.toIntExact(message.entityId()));
                log.info("Discarded {} translation jobs of {}/{}", deleted, message.entityType(), message.entityId());
            }
            default -> log.warn("Discarding translation command with unknown action {}", message.action());
        }
    }
}
