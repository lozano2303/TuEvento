package com.capysoft.notification.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.capysoft.notification.application.port.out.NotificationTranslationReader.TranslationView;

/**
 * El lector de traducciones contra una base real (H2): mismas columnas que crean las
 * migraciones del monolito para language y notification_translation.
 */
@JdbcTest
@Import(JdbcNotificationTranslationReader.class)
@DisplayName("JdbcNotificationTranslationReader")
class JdbcNotificationTranslationReaderTest {

    @Autowired JdbcNotificationTranslationReader reader;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void schema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS language (language_id INT PRIMARY KEY, code VARCHAR(10) NOT NULL)");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS notification_translation (
                  translation_id INT PRIMARY KEY, notification_id INT NOT NULL, language_id INT NOT NULL,
                  translated_subject VARCHAR(100), translated_body VARCHAR(255),
                  source VARCHAR(50) NOT NULL, status VARCHAR(20) NOT NULL)
                """);
        jdbc.update("DELETE FROM notification_translation");
        jdbc.update("DELETE FROM language");
        jdbc.update("INSERT INTO language VALUES (1, 'es'), (2, 'en'), (3, 'pt')");
        jdbc.update("INSERT INTO notification_translation VALUES (10, 42, 2, 'Payment approved', 'Approved', 'AUTOMATIC', 'PUBLISHED')");
        jdbc.update("INSERT INTO notification_translation VALUES (11, 42, 3, NULL, NULL, 'AUTOMATIC', 'PENDING')");
        jdbc.update("INSERT INTO notification_translation VALUES (12, 99, 2, 'Other', 'Other', 'AUTOMATIC', 'PUBLISHED')");
    }

    @Test
    @DisplayName("devuelve las traducciones de la notificación con el código de su idioma")
    void findsAllTranslationsOfANotification() {
        assertThat(reader.findAll(42L)).extracting(TranslationView::languageCode).containsExactlyInAnyOrder("en", "pt");
    }

    @Test
    @DisplayName("devuelve la traducción a un idioma y si está publicada")
    void findsOneTranslation() {
        assertThat(reader.find(42L, 2L)).hasValueSatisfying(t -> {
            assertThat(t.subject()).isEqualTo("Payment approved");
            assertThat(t.isPublished()).isTrue();
        });
        assertThat(reader.find(42L, 3L)).hasValueSatisfying(t -> assertThat(t.isPublished()).isFalse());
        assertThat(reader.find(42L, 1L)).isEmpty();
    }

    @Test
    @DisplayName("resuelve el código de un idioma")
    void resolvesLanguageCode() {
        assertThat(reader.languageCode(2L)).contains("en");
        assertThat(reader.languageCode(9L)).isEmpty();
    }
}
