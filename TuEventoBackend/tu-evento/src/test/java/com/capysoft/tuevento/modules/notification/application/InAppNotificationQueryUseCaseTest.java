package com.capysoft.tuevento.modules.notification.application;

import com.capysoft.tuevento.modules.notification.application.dto.InAppNotificationDetails;
import com.capysoft.tuevento.modules.notification.application.dto.InAppNotificationResponse;
import com.capysoft.tuevento.modules.notification.application.dto.NotificationTranslationDTO;
import com.capysoft.tuevento.modules.notification.application.usecase.InAppNotificationQueryUseCase;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationTranslationRepository;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationUserRepository;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository.NotificationUserRepositoryAdapter;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La bandeja de notificaciones devuelve cada notificación con sus traducciones, identificadas
 * por el código del idioma.
 */
@DisplayName("InAppNotificationQueryUseCase — listado con traducciones")
class InAppNotificationQueryUseCaseTest {

    private final NotificationUserRepositoryAdapter details = mock(NotificationUserRepositoryAdapter.class);
    private final NotificationTranslationRepository translations = mock(NotificationTranslationRepository.class);
    private final LanguageCatalog languages = mock(LanguageCatalog.class);
    private final InAppNotificationQueryUseCase useCase = new InAppNotificationQueryUseCase(
            mock(NotificationUserRepository.class), details, translations, languages);

    @Test
    @DisplayName("cada traducción lleva el código de su idioma; las de un idioma desconocido se omiten")
    void listsWithTranslations() {
        when(details.findInAppDetailsWithNotificationByUserId(70, false, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(InAppNotificationDetails.builder()
                        .notificationUserId(1L).notificationId(9L).subject("Entrada lista").build())));
        when(translations.findAllByNotificationId(9)).thenReturn(List.of(
                NotificationTranslation.builder().languageId(2).translatedSubject("Ticket ready")
                        .source(TranslationSource.MACHINE).status(TranslationStatus.PUBLISHED).build(),
                NotificationTranslation.builder().languageId(8).translatedSubject("?").build()));
        when(languages.findById(2)).thenReturn(Optional.of(LanguageSummary.builder().languageId(2).code("en").build()));
        when(languages.findById(8)).thenReturn(Optional.empty());

        InAppNotificationResponse response = useCase.listMine(70, 0, 0, false).getContent().get(0);

        assertThat(response.getSubject()).isEqualTo("Entrada lista");
        assertThat(response.getTranslations()).extracting(NotificationTranslationDTO::getLanguageCode,
                        NotificationTranslationDTO::getTranslatedSubject, NotificationTranslationDTO::getStatus)
                .containsExactly(tuple("en", "Ticket ready", "PUBLISHED"));
    }
}
