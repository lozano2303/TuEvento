package com.capysoft.tuevento.modules.profile.infrastructure;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.profile.infrastructure.translation.ProfileTranslatableContentHandler;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Guardado de la traducción de la biografía de un perfil. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileTranslatableContentHandler — saveTranslation")
class ProfileTranslatableContentHandlerTest {

    @Mock private ProfileRepository            profileRepository;
    @Mock private ProfileTranslationRepository profileTranslationRepository;
    @Mock private LanguageRepository           languageRepository;

    @InjectMocks private ProfileTranslatableContentHandler handler;

    @Test
    @DisplayName("Actualiza la traducción existente con la bio, fuente y estado")
    void updates_existing_translation() {
        ProfileTranslation existing = new ProfileTranslation();
        existing.setProfileId(30);
        existing.setLanguageId(3);
        when(languageRepository.findByCode("pt")).thenReturn(Optional.of(Language.builder().languageId(3).code("pt").build()));
        when(profileTranslationRepository.findByProfileAndLanguage(30L, 3)).thenReturn(Optional.of(existing));

        handler.saveTranslation(30L, "pt", Map.of("bio", "Amo eventos"),
                TranslationSource.MANUAL, TranslationStatus.PUBLISHED);

        ArgumentCaptor<ProfileTranslation> captor = ArgumentCaptor.forClass(ProfileTranslation.class);
        verify(profileTranslationRepository).save(captor.capture());
        ProfileTranslation saved = captor.getValue();
        assertSame(existing, saved);
        assertEquals("Amo eventos", saved.getTranslatedBio());
        assertEquals(TranslationSource.MANUAL, saved.getSource());
        assertEquals(TranslationStatus.PUBLISHED, saved.getStatus());
    }
}
