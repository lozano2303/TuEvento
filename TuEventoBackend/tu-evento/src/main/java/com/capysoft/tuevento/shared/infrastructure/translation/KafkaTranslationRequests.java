package com.capysoft.tuevento.shared.infrastructure.translation;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Las traducciones las hace el servicio de localización. Las peticiones (con el texto original) y
 * los descartes se le envían por {@link TranslationTopics#COMMANDS}; el estado de los trabajos se
 * lee de translation_job, en la misma base de datos.
 *
 * <p>Un fallo al publicar se registra y no rompe el flujo que lo originó: una petición perdida se
 * vuelve a hacer la próxima vez que se lea la entidad en otro idioma.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTranslationRequests implements TranslationRequests {

    private static final String JOB_STATUS = """
            SELECT status FROM translation_job
            WHERE entity_type = ? AND entity_id = ? AND target_language_id = ?""";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final TranslatableContents contents;

    @Override
    public void request(String entityType, long entityId) {
        Map<String, String> texts = contents.sourceTexts(entityType, entityId);
        if (texts.isEmpty()) {
            log.debug("Nothing to translate for {}/{}", entityType, entityId);
            return;
        }
        Map<String, Object> command = command("REQUEST", entityType, entityId);
        command.put("sourceLanguage", SOURCE_LANGUAGE);
        command.put("texts", texts);
        publish(entityType, entityId, command);
    }

    @Override
    public Optional<TranslationJobStatus> jobStatus(String entityType, long entityId, int languageId) {
        return jdbcTemplate.queryForList(JOB_STATUS, String.class, entityType, entityId, languageId).stream()
                .findFirst()
                .map(TranslationJobStatus::valueOf);
    }

    @Override
    public int discard(String entityType, long entityId) {
        publish(entityType, entityId, command("DISCARD", entityType, entityId));
        return 0;
    }

    private static Map<String, Object> command(String action, String entityType, long entityId) {
        Map<String, Object> command = new HashMap<>();
        command.put("action", action);
        command.put("entityType", entityType);
        command.put("entityId", entityId);
        return command;
    }

    private void publish(String entityType, long entityId, Map<String, Object> command) {
        try {
            kafkaTemplate.send(TranslationTopics.COMMANDS, TranslationTopics.key(entityType, entityId),
                    objectMapper.writeValueAsString(command));
        } catch (Exception e) {
            log.error("Could not publish translation {} for {}/{}", command.get("action"), entityType, entityId, e);
        }
    }
}
