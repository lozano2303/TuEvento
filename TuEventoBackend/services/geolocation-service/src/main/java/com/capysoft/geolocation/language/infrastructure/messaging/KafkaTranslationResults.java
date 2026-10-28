package com.capysoft.geolocation.language.infrastructure.messaging;

import com.capysoft.geolocation.language.application.port.out.TranslationResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Entrega cada traducción terminada al monolito por {@link TranslationTopics#RESULTS}, que la
 * guarda en la tabla del dominio dueño. Un fallo al publicar se propaga: el procesador marca el
 * trabajo como fallido y lo reintenta, en vez de darlo por terminado sin entregar.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "translation.enabled", havingValue = "true")
public class KafkaTranslationResults implements TranslationResults {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void deliver(String entityType, long entityId, String languageCode, Map<String, String> translatedTexts) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new TranslationResultMessage(entityType, entityId, languageCode, translatedTexts));
            kafkaTemplate.send(TranslationTopics.RESULTS, TranslationTopics.key(entityType, entityId), payload).join();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Translation result cannot be serialized", e);
        }
    }
}
