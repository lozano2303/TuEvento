package com.capysoft.tuevento.modules.profile.application.usecase;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.mapper.ProfileResponseAssembler;
import com.capysoft.tuevento.modules.profile.application.port.out.CityLookupPort;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * El perfil se devuelve con la biografía traducida si hay una publicada; si no, se pide la
 * traducción bajo demanda y se indica si se está generando.
 */
@DisplayName("GetProfileByUserIdUseCase")
class GetProfileByUserIdUseCaseTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final CityLookupPort cities = mock(CityLookupPort.class);
    private final LanguageRepository languages = mock(LanguageRepository.class);
    private final ProfileTranslationRepository translations = mock(ProfileTranslationRepository.class);
    private final TranslationRequests requests = mock(TranslationRequests.class);
    private final GetProfileByUserIdUseCase useCase = new GetProfileByUserIdUseCase(
            profiles, new ProfileResponseAssembler(cities), languages, translations, requests);

    private static Profile profile(String bio) {
        return Profile.builder().profileId(7L).userId(70).fullName("Ana").bio(bio).build();
    }

    private static ProfileTranslation translation(TranslationStatus status) {
        ProfileTranslation t = new ProfileTranslation();
        t.setProfileId(7);
        t.setLanguageId(2);
        t.setTranslatedBio("I like music");
        t.setStatus(status);
        return t;
    }

    @BeforeEach
    void data() {
        when(cities.findCity(any())).thenReturn(Optional.empty());
        when(profiles.findByUserId(70)).thenReturn(Optional.of(profile("Me gusta la música")));
        when(languages.findByCode("en")).thenReturn(Optional.of(Language.builder().languageId(2).code("en").build()));
    }

    @Test
    @DisplayName("usuario sin perfil: 404")
    void notFound() {
        when(profiles.findByUserId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getByUserId(99)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("sin idioma, en español o con un idioma desconocido no traduce")
    void untranslated() {
        when(languages.findByCode("xx")).thenReturn(Optional.empty());

        assertThat(useCase.getByUserId(70).getBio()).isEqualTo("Me gusta la música");
        assertThat(useCase.getByUserId(70, "es").getTranslatedBio()).isNull();
        assertThat(useCase.getByUserId(70, "xx").getTranslationGenerating()).isFalse();
        verifyNoInteractions(requests);
    }

    @Test
    @DisplayName("con traducción publicada la devuelve sin pedir otra")
    void published() {
        when(translations.findByProfileAndLanguage(7L, 2)).thenReturn(Optional.of(translation(TranslationStatus.PUBLISHED)));

        ProfileResponse response = useCase.getByUserId(70, "en");

        assertThat(response.getTranslatedBio()).isEqualTo("I like music");
        assertThat(response.getLanguageCode()).isEqualTo("en");
        assertThat(response.getTranslationGenerating()).isFalse();
        verifyNoInteractions(requests);
    }

    @Test
    @DisplayName("sin traducción publicada la pide bajo demanda e indica si se está generando")
    void onDemand() {
        when(translations.findByProfileAndLanguage(7L, 2)).thenReturn(Optional.of(translation(TranslationStatus.DRAFT)));
        when(requests.ensureRequested("profile", 7L, 2)).thenReturn(true);

        ProfileResponse response = useCase.getByUserId(70, "en");

        assertThat(response.getTranslatedBio()).isNull();
        assertThat(response.getTranslationGenerating()).isTrue();
    }

    @Test
    @DisplayName("un perfil sin biografía no pide traducción; un fallo deja el perfil sin traducir")
    void noBioAndFailure() {
        when(profiles.findByUserId(71)).thenReturn(Optional.of(profile(" ")));
        when(translations.findByProfileAndLanguage(7L, 2)).thenReturn(Optional.empty());

        assertThat(useCase.getByUserId(71, "en").getTranslationGenerating()).isFalse();
        verify(requests, never()).ensureRequested(anyString(), anyLong(), anyInt());

        when(requests.ensureRequested("profile", 7L, 2)).thenThrow(new IllegalStateException("bd caída"));
        assertThat(useCase.getByUserId(70, "en").getTranslationGenerating()).isFalse();
    }
}
