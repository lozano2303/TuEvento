package com.capysoft.tuevento.modules.notification.infrastructure.translation;

import com.capysoft.tuevento.modules.notification.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationTranslationRepository;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository.JpaNotificationRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Guardado y estado de la traducción de una notificación. */
@DisplayName("NotificationTranslatableContentHandler")
class NotificationTranslatableContentHandlerTest {

    private final NotificationTranslationRepository translations = mock(NotificationTranslationRepository.class);
    private final LanguageCatalog languages = mock(LanguageCatalog.class);
    private final NotificationTranslatableContentHandler handler =
            new NotificationTranslatableContentHandler(mock(JpaNotificationRepository.class), translations, languages);

    @BeforeEach
    void languages() {
        when(languages.findByCode("en")).thenReturn(Optional.of(LanguageSummary.builder().languageId(2).code("en").build()));
        when(languages.findByCode("xx")).thenReturn(Optional.empty());
        when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("crea la traducción con asunto, cuerpo, origen y estado")
    void creates() {
        when(translations.findByNotificationAndLanguage(9, 2)).thenReturn(Optional.empty());

        handler.saveTranslation(9L, "en", Map.of("subject", "Ticket ready", "body", "<p>Enjoy</p>"),
                TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        ArgumentCaptor<NotificationTranslation> saved = ArgumentCaptor.forClass(NotificationTranslation.class);
        verify(translations).save(saved.capture());
        assertThat(saved.getValue()).extracting(NotificationTranslation::getNotificationId,
                        NotificationTranslation::getLanguageId, NotificationTranslation::getTranslatedSubject,
                        NotificationTranslation::getTranslatedBody, NotificationTranslation::getStatus)
                .containsExactly(9, 2, "Ticket ready", "<p>Enjoy</p>", TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("sin textos útiles o con idioma desconocido no guarda")
    void skips() {
        when(translations.findByNotificationAndLanguage(9, 2)).thenReturn(Optional.empty());

        handler.saveTranslation(9L, "en", Map.of("subject", ""), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
        handler.saveTranslation(9L, "xx", Map.of("subject", "Hi"), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        verify(translations, never()).save(any());
    }

    @Test
    @DisplayName("estado de la traducción, o vacío si el idioma no existe")
    void status() {
        when(translations.findByNotificationAndLanguage(9, 2))
                .thenReturn(Optional.of(NotificationTranslation.builder().status(TranslationStatus.PUBLISHED).build()));

        assertThat(handler.findTranslationStatus(9L, "en")).contains(TranslationStatus.PUBLISHED);
        assertThat(handler.findTranslationStatus(9L, "xx")).isEmpty();
    }
}
