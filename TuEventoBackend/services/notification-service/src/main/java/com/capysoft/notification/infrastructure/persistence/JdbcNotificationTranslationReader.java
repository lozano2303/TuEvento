package com.capysoft.notification.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.capysoft.notification.application.port.out.NotificationTranslationReader;

import lombok.RequiredArgsConstructor;

/**
 * Lee notification_translation y language con SQL de solo lectura, sin entidades JPA: esas
 * tablas son del sistema de traducciones del monolito y su esquema lo migra él.
 */
@Component
@RequiredArgsConstructor
public class JdbcNotificationTranslationReader implements NotificationTranslationReader {

    private static final String SELECT = """
            SELECT l.code, t.translated_subject, t.translated_body, t.status, t.source
            FROM notification_translation t
            JOIN language l ON l.language_id = t.language_id
            WHERE t.notification_id = ?
            """;

    private static final RowMapper<TranslationView> ROW = (rs, i) -> new TranslationView(
            rs.getString("code"), rs.getString("translated_subject"), rs.getString("translated_body"),
            rs.getString("status"), rs.getString("source"));

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<TranslationView> findAll(Long notificationId) {
        return jdbcTemplate.query(SELECT, ROW, notificationId);
    }

    @Override
    public Optional<TranslationView> find(Long notificationId, Long languageId) {
        return jdbcTemplate.query(SELECT + " AND t.language_id = ?", ROW, notificationId, languageId).stream().findFirst();
    }

    @Override
    public Optional<String> languageCode(Long languageId) {
        return jdbcTemplate.query("SELECT code FROM language WHERE language_id = ?",
                (rs, i) -> rs.getString("code"), languageId).stream().findFirst();
    }
}
