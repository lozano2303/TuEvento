package com.capysoft.tuevento.modules.profile.application.listener;

import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.modules.profile.domain.event.ProfileUpdatedEvent;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileLogRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileTranslationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Solo un cambio de biografía invalida las traducciones y trabajos pendientes del perfil.
 */
@DisplayName("ProfileTranslationListener")
class ProfileTranslationListenerTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 27, 12, 0);

    private final ProfileTranslationRepository translations = mock(ProfileTranslationRepository.class);
    private final TranslationJobRepository jobs = mock(TranslationJobRepository.class);
    private final ProfileLogRepository logs = mock(ProfileLogRepository.class);
    private final ProfileTranslationListener listener = new ProfileTranslationListener(translations, jobs, logs);

    private static ProfileUpdatedEvent updated() {
        return ProfileUpdatedEvent.builder().profileId(7L).occurredAt(AT).build();
    }

    private void bioChanged(boolean changed) {
        when(logs.existsByProfileIdAndActionAndOccurredAtBetween(7L, "BIO_CHANGED",
                AT.minusSeconds(5), AT.plusSeconds(5))).thenReturn(changed);
    }

    @Test
    @DisplayName("con la biografía cambiada borra traducciones y trabajos del perfil")
    void bioChangedInvalidates() {
        bioChanged(true);

        listener.onProfileUpdated(updated());

        verify(translations).deleteByProfile(7L);
        verify(jobs).deleteByEntity("profile", 7);
    }

    @Test
    @DisplayName("sin cambio de biografía conserva las traducciones")
    void otherChangesKeepTranslations() {
        bioChanged(false);

        listener.onProfileUpdated(updated());

        verify(translations, never()).deleteByProfile(anyLong());
        verify(jobs, never()).deleteByEntity(anyString(), any());
    }

    @Test
    @DisplayName("si no puede consultar el historial asume que la biografía cambió")
    void logFailureAssumesChange() {
        when(logs.existsByProfileIdAndActionAndOccurredAtBetween(eq(7L), anyString(), any(), any()))
                .thenThrow(new IllegalStateException("bd caída"));

        listener.onProfileUpdated(updated());

        verify(translations).deleteByProfile(7L);
    }

    @Test
    @DisplayName("un fallo al borrar no se propaga")
    void failureIsSwallowed() {
        bioChanged(true);
        doThrow(new IllegalStateException("bd caída")).when(translations).deleteByProfile(7L);

        assertThatCode(() -> listener.onProfileUpdated(updated())).doesNotThrowAnyException();
    }
}
