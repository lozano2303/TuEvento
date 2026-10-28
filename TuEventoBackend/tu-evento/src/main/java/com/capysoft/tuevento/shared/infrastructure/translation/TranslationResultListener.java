package com.capysoft.tuevento.shared.infrastructure.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Guarda en cada dominio las traducciones que termina el servicio de localización. Recibir dos
 * veces la misma traducción es inocuo (la segunda ve la primera ya publicada y no la toca). Un
 * mensaje que no se puede leer se registra y se descarta.
 *
 * <p>Kafka no autentica a quien publica: solo se acepta un resultado si existe un trabajo de
 * traducción en curso o terminado para esa entidad e idioma, así un mensaje inventado no puede
 * escribir traducciones de entidades que nadie pidió traducir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationResultListener {

    private static final String JOB_EXISTS = """
            SELECT count(*) FROM translation_job j JOIN language l ON l.language_id = j.target_language_id
            WHERE j.entity_type = ? AND j.entity_id = ? AND l.code = ? AND j.status IN ('PROCESSING', 'COMPLETED')""";

    private final TranslatableContents contents;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

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
        Integer jobs = jdbcTemplate.queryForObject(JOB_EXISTS, Integer.class,
                result.entityType(), result.entityId(), result.languageCode());
        if (jobs == null || jobs == 0) {
            log.warn("Discarding translation result without a translation job: {}/{} {}",
                    result.entityType(), result.entityId(), result.languageCode());
            return;
        }
        contents.saveMachineTranslation(result.entityType(), result.entityId(), result.languageCode(), result.texts());
    }
}
