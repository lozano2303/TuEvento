package com.capysoft.tuevento.shared.infrastructure.notification;

import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.application.translation.TranslatableContentHandler;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Traducciones del asunto y el cuerpo de las notificaciones. Las notificaciones las crea
 * notification-service en la misma base de datos; el monolito solo lee su texto y guarda las
 * traducciones en notification_translation. Un texto vacío no se traduce ni se guarda.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTranslatableContentHandler implements TranslatableContentHandler {

    static final String ENTITY_TYPE = "notification";

    private static final String SOURCE = "SELECT subject, body FROM notification WHERE notification_id = ?";
    private static final String STATUS = """
            SELECT status FROM notification_translation WHERE notification_id = ? AND language_id = ?""";
    // Solo pisa los campos que llegan con texto: los demás conservan lo que ya había
    private static final String UPSERT = """
            INSERT INTO notification_translation
                (notification_id, language_id, translated_subject, translated_body, source, status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (notification_id, language_id) DO UPDATE SET
                translated_subject = COALESCE(EXCLUDED.translated_subject, notification_translation.translated_subject),
                translated_body = COALESCE(EXCLUDED.translated_body, notification_translation.translated_body),
                source = EXCLUDED.source,
                status = EXCLUDED.status""";

    private final JdbcTemplate jdbcTemplate;
    private final LanguageCatalog languageCatalog;

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Map<String, String> loadSourceTexts(Long entityId) {
        return jdbcTemplate.query(SOURCE, (rs, row) -> {
            Map<String, String> texts = new LinkedHashMap<>();
            putIfText(texts, "subject", rs.getString("subject"));
            putIfText(texts, "body", rs.getString("body"));
            return texts;
        }, entityId).stream().findFirst().orElseGet(() -> {
            log.warn("Notification {} not found for translation", entityId);
            return Map.of();
        });
    }

    @Override
    public void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts,
                                TranslationSource source, TranslationStatus status) {
        Optional<Integer> languageId = languageCatalog.findByCode(languageCode).map(LanguageSummary::getLanguageId);
        if (languageId.isEmpty()) {
            log.error("Language {} not found: translation of notification {} not saved", languageCode, entityId);
            return;
        }
        String subject = textOrNull(translatedTexts.get("subject"));
        String body = textOrNull(translatedTexts.get("body"));
        if (subject == null && body == null) {
            log.warn("No translated text for notification {} in {}: nothing saved", entityId, languageCode);
            return;
        }
        jdbcTemplate.update(UPSERT, entityId, languageId.get(), subject, body, source.name(), status.name());
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        return languageCatalog.findByCode(languageCode)
                .flatMap(language -> jdbcTemplate.queryForList(STATUS, String.class, entityId,
                        language.getLanguageId()).stream().findFirst())
                .map(status -> TranslationStatus.valueOf(status.toUpperCase()));
    }

    private static void putIfText(Map<String, String> texts, String field, String value) {
        if (textOrNull(value) != null) {
            texts.put(field, value);
        }
    }

    private static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
