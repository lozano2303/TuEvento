package com.capysoft.identity.profile.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.identity.profile.application.port.out.LanguageDirectoryPort;
import com.capysoft.identity.profile.domain.model.Profile;
import com.capysoft.identity.profile.domain.model.ProfileTranslation;
import com.capysoft.identity.profile.domain.repository.ProfileRepository;
import com.capysoft.identity.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.identity.shared.domain.valueobject.TranslationSource;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;

@DisplayName("InternalProfileController (motor de traducciones y avatar del monolito)")
class InternalProfileControllerTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final ProfileTranslationRepository translations = mock(ProfileTranslationRepository.class);
    private final LanguageDirectoryPort languages = mock(LanguageDirectoryPort.class);
    private final InternalProfileController controller = new InternalProfileController(profiles, translations, languages);

    @Test
    @DisplayName("textos traducibles: la biografía si tiene contenido; nada si está vacía o el perfil no existe")
    void translatableTexts() {
        when(profiles.findById(30L)).thenReturn(Optional.of(Profile.builder().profileId(30L).bio("Hola").build()));
        when(profiles.findById(31L)).thenReturn(Optional.of(Profile.builder().profileId(31L).bio("  ").build()));

        assertThat(controller.translatableTexts(30L).getBody().getData()).isEqualTo(Map.of("bio", "Hola"));
        assertThat(controller.translatableTexts(31L).getBody().getData()).isEmpty();
        assertThat(controller.translatableTexts(32L).getBody().getData()).isEmpty();
    }

    @Test
    @DisplayName("guarda la traducción nueva con idioma, fuente y estado; idioma desconocido → error de negocio")
    void savesTranslation() {
        when(languages.findIdByCode("en")).thenReturn(Optional.of(2));
        when(translations.findByProfileAndLanguage(30L, 2)).thenReturn(Optional.empty());

        controller.saveTranslation(30L, "en", new InternalProfileController.TranslationRequest(
                Map.of("bio", "Hello"), TranslationSource.MACHINE, TranslationStatus.PUBLISHED));

        ArgumentCaptor<ProfileTranslation> saved = ArgumentCaptor.forClass(ProfileTranslation.class);
        verify(translations).save(saved.capture());
        assertThat(saved.getValue().getProfileId()).isEqualTo(30);
        assertThat(saved.getValue().getLanguageId()).isEqualTo(2);
        assertThat(saved.getValue().getTranslatedBio()).isEqualTo("Hello");
        assertThat(saved.getValue().getStatus()).isEqualTo(TranslationStatus.PUBLISHED);

        assertThatThrownBy(() -> controller.saveTranslation(30L, "xx",
                new InternalProfileController.TranslationRequest(Map.of(), TranslationSource.MACHINE, TranslationStatus.DRAFT)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("asigna el avatar por defecto solo a los perfiles sin avatar")
    void assignsDefaultAvatar() {
        Profile withoutAvatar = Profile.builder().profileId(30L).build();
        when(profiles.findAllWithNullStoredFileId()).thenReturn(List.of(withoutAvatar));

        assertThat(controller.assignDefaultAvatar(1).getBody().getData()).isEqualTo(1);
        assertThat(withoutAvatar.getStoredFileId()).isEqualTo(1);
        verify(profiles).save(withoutAvatar);

        when(profiles.findAllWithNullStoredFileId()).thenReturn(List.of());
        assertThat(controller.assignDefaultAvatar(1).getBody().getData()).isZero();
        verify(profiles, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    @DisplayName("estado de la traducción, o null si no existe")
    void status() {
        ProfileTranslation published = new ProfileTranslation(30, 2, "Hello", TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
        when(languages.findIdByCode("en")).thenReturn(Optional.of(2));
        when(translations.findByProfileAndLanguage(30L, 2)).thenReturn(Optional.of(published));

        assertThat(controller.translationStatus(30L, "en").getBody().getData().status()).isEqualTo(TranslationStatus.PUBLISHED);
        assertThat(controller.translationStatus(30L, "xx").getBody().getData().status()).isNull();
        verify(translations, never()).findByProfileAndLanguage(30L, null);
    }
}
