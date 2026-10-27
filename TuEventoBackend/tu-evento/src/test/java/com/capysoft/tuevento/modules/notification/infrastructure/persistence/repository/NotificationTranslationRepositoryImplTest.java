package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationTranslationEntity;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.mapper.NotificationTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adaptador de persistencia de traducciones de notificaciones con el mapper real y el repositorio JPA simulado.
 */
@DisplayName("NotificationTranslationRepositoryImpl")
class NotificationTranslationRepositoryImplTest {

    private final NotificationTranslationJpaRepository jpa = mock(NotificationTranslationJpaRepository.class);
    private final NotificationTranslationRepositoryImpl repository =
            new NotificationTranslationRepositoryImpl(jpa, new NotificationTranslationMapper());

    private static NotificationTranslationEntity entity(int notificationId, int languageId) {
        return NotificationTranslationEntity.builder().translationId(notificationId * 10).notificationId(notificationId)
                .languageId(languageId).translatedSubject("Ticket confirmed").translatedBody("<p>Enjoy</p>")
                .source(TranslationSource.MACHINE).status(TranslationStatus.PUBLISHED).build();
    }

    @Test
    @DisplayName("busca por notificación e idioma y todas las de una notificación")
    void finds() {
        when(jpa.findByNotificationIdAndLanguageId(8, 2)).thenReturn(Optional.of(entity(8, 2)));
        when(jpa.findByNotificationId(8)).thenReturn(List.of(entity(8, 2), entity(8, 3)));

        NotificationTranslation found = repository.findByNotificationAndLanguage(8, 2).orElseThrow();
        assertThat(found.getTranslationId()).isEqualTo(80);
        assertThat(found.getNotificationId()).isEqualTo(8);
        assertThat(found.getLanguageId()).isEqualTo(2);
        assertThat(found.getTranslatedSubject()).isEqualTo("Ticket confirmed");
        assertThat(found.getTranslatedBody()).isEqualTo("<p>Enjoy</p>");
        assertThat(found.getSource()).isEqualTo(TranslationSource.MACHINE);
        assertThat(found.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
        assertThat(repository.findAllByNotificationId(8))
                .extracting(NotificationTranslation::getLanguageId).containsExactly(2, 3);
    }

    @Test
    @DisplayName("guarda todos los campos y devuelve lo guardado")
    void saves() {
        when(jpa.save(any())).thenAnswer(inv -> {
            NotificationTranslationEntity saved = inv.getArgument(0);
            saved.setTranslationId(99);
            return saved;
        });
        NotificationTranslation translation = NotificationTranslation.builder().notificationId(8).languageId(3)
                .translatedSubject("Billet confirmé").translatedBody("<p>Profitez</p>")
                .source(TranslationSource.MANUAL).status(TranslationStatus.DRAFT).build();

        assertThat(repository.save(translation).getTranslationId()).isEqualTo(99);

        ArgumentCaptor<NotificationTranslationEntity> captor = ArgumentCaptor.forClass(NotificationTranslationEntity.class);
        verify(jpa).save(captor.capture());
        NotificationTranslationEntity written = captor.getValue();
        assertThat(written.getNotificationId()).isEqualTo(8);
        assertThat(written.getLanguageId()).isEqualTo(3);
        assertThat(written.getTranslatedSubject()).isEqualTo("Billet confirmé");
        assertThat(written.getTranslatedBody()).isEqualTo("<p>Profitez</p>");
        assertThat(written.getSource()).isEqualTo(TranslationSource.MANUAL);
        assertThat(written.getStatus()).isEqualTo(TranslationStatus.DRAFT);
    }
}
