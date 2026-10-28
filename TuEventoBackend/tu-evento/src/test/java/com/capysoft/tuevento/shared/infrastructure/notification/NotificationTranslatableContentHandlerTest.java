package com.capysoft.tuevento.shared.infrastructure.notification;

import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("NotificationTranslatableContentHandler")
class NotificationTranslatableContentHandlerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final LanguageCatalog languages = mock(LanguageCatalog.class);
    private final NotificationTranslatableContentHandler handler = new NotificationTranslatableContentHandler(jdbc, languages);

    @BeforeEach
    void english() {
        when(languages.findByCode("en")).thenReturn(Optional.of(LanguageSummary.builder().languageId(2).code("en").build()));
        when(languages.findByCode("xx")).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("traduce el tipo de entidad notification")
    void entityType() {
        assertThat(handler.entityType()).isEqualTo("notification");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("lee asunto y cuerpo de la notificación, sin los textos vacíos")
    void loadsSourceTexts() throws Exception {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(7L))).thenAnswer(i -> {
            ResultSet row = mock(ResultSet.class);
            when(row.getString("subject")).thenReturn("Evento cancelado");
            when(row.getString("body")).thenReturn("  ");
            return List.of(((RowMapper<Map<String, String>>) i.getArgument(1)).mapRow(row, 0));
        });
        when(jdbc.query(anyString(), any(RowMapper.class), eq(8L))).thenReturn(List.of());

        assertThat(handler.loadSourceTexts(7L)).containsExactly(Map.entry("subject", "Evento cancelado"));
        assertThat(handler.loadSourceTexts(8L)).isEmpty();
    }

    @Test
    @DisplayName("guarda la traducción con asunto, cuerpo, origen y estado (upsert por notificación e idioma)")
    void saves() {
        handler.saveTranslation(7L, "en", Map.of("subject", "Event cancelled", "body", "Sorry"),
                TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), eq(7L), eq(2), eq("Event cancelled"), eq("Sorry"), eq("MACHINE"),
                eq("PUBLISHED"));
        assertThat(sql.getValue()).contains("ON CONFLICT (notification_id, language_id)");
    }

    @Test
    @DisplayName("un campo vacío llega como null para conservar el que ya había")
    void keepsMissingField() {
        handler.saveTranslation(7L, "en", Map.of("subject", "Event cancelled", "body", " "),
                TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        verify(jdbc).update(anyString(), eq(7L), eq(2), eq("Event cancelled"), eq(null), eq("MACHINE"), eq("PUBLISHED"));
    }

    @Test
    @DisplayName("sin textos útiles o con idioma desconocido no guarda")
    void skips() {
        handler.saveTranslation(7L, "en", Map.of("subject", " "), TranslationSource.MACHINE, TranslationStatus.DRAFT);
        handler.saveTranslation(7L, "xx", Map.of("subject", "Hi"), TranslationSource.MACHINE, TranslationStatus.DRAFT);

        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("estado de la traducción (también el 'draft' en minúscula por defecto), o vacío si no existe")
    void status() {
        when(jdbc.queryForList(anyString(), eq(String.class), eq(7L), eq(2))).thenReturn(List.of("PUBLISHED"));
        when(jdbc.queryForList(anyString(), eq(String.class), eq(9L), eq(2))).thenReturn(List.of("draft"));
        when(jdbc.queryForList(anyString(), eq(String.class), eq(8L), eq(2))).thenReturn(List.of());

        assertThat(handler.findTranslationStatus(7L, "en")).contains(TranslationStatus.PUBLISHED);
        assertThat(handler.findTranslationStatus(9L, "en")).contains(TranslationStatus.DRAFT);
        assertThat(handler.findTranslationStatus(8L, "en")).isEmpty();
        assertThat(handler.findTranslationStatus(7L, "xx")).isEmpty();
    }
}
