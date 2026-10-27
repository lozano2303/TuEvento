package com.capysoft.tuevento.shared.infrastructure.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Modo {@code remote}: guarda en cada dominio las traducciones que termina el servicio de
 * localización. Recibir dos veces la misma traducción es inocuo (la segunda ve la primera ya
 * publicada y no la toca). Un mensaje que no se puede leer se registra y se descarta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "translation.mode", havingValue = "remote")
public class TranslationResultListener {

    private final TranslatableContents contents;
    private final ObjectMapper objectMapper;

    /** Traducción terminada de una entidad a un idioma. */
    record TranslationResultMessage(String entityType, Long entityId, String languageCode, Map<String, String> texts) {
    }

    @KafkaListener(topics = TranslationTopics.RESULTS, groupId = "monolito-translation-results")
    public void onResult(String rawMessage) {
        TranslationResultMessage result;
        try {
            result = objectMapper.readValue(rawMessage, TranslationResultMessage.class);
        } catch (Exception e) {
            log.error("Discarding unreadable translation result", e);
            return;
        }
        if (result.entityType() == null || result.entityId() == null || result.languageCode() == null
                || result.texts() == null) {
            log.warn("Discarding incomplete translation result for {}/{}", result.entityType(), result.entityId());
            return;
        }
        contents.saveMachineTranslation(result.entityType(), result.entityId(), result.languageCode(), result.texts());
    }
}
