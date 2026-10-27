package com.capysoft.identity.profile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.identity.profile.application.dto.response.ProfileResponse;
import com.capysoft.identity.profile.application.mapper.ProfileResponseAssembler;
import com.capysoft.identity.profile.application.port.out.CityLookupPort;
import com.capysoft.identity.profile.application.port.out.LanguageDirectoryPort;
import com.capysoft.identity.profile.application.port.out.TranslationJobsPort;
import com.capysoft.identity.profile.application.port.out.TranslationJobsPort.JobState;
import com.capysoft.identity.profile.domain.model.Profile;
import com.capysoft.identity.profile.domain.model.ProfileTranslation;
import com.capysoft.identity.profile.domain.repository.ProfileRepository;
import com.capysoft.identity.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.identity.shared.domain.valueobject.TranslationSource;
import com.capysoft.identity.shared.domain.valueobject.TranslationStatus;

/** Traducción bajo demanda de la biografía al pedir un perfil en otro idioma. */
@DisplayName("GetProfileByUserIdUseCase — traducción de la biografía")
class GetProfileByUserIdTranslationTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final LanguageDirectoryPort languages = mock(LanguageDirectoryPort.class);
    private final ProfileTranslationRepository translations = mock(ProfileTranslationRepository.class);
    private final TranslationJobsPort jobs = mock(TranslationJobsPort.class);
    private final GetProfileByUserIdUseCase useCase = new GetProfileByUserIdUseCase(profiles,
            new ProfileResponseAssembler(mock(CityLookupPort.class)), languages, translations, jobs);

    @BeforeEach
    void profileWithBio() {
        when(profiles.findByUserId(7)).thenReturn(Optional.of(
                Profile.builder().profileId(30L).userId(7).bio("Amo los eventos").build()));
        when(languages.findIdByCode("en")).thenReturn(Optional.of(2));
    }

    @Test
    @DisplayName("devuelve la traducción publicada sin pedir otra")
    void returnsPublishedTranslation() {
        when(translations.findByProfileAndLanguage(30L, 2)).thenReturn(Optional.of(new ProfileTranslation(
                30, 2, "I love events", TranslationSource.MACHINE, TranslationStatus.PUBLISHED)));

        ProfileResponse response = useCase.getByUserId(7, "en");

        assertThat(response.getTranslatedBio()).isEqualTo("I love events");
        assertThat(response.getLanguageCode()).isEqualTo("en");
        assertThat(response.getTranslationGenerating()).isFalse();
        verifyNoInteractions(jobs);
    }

    @Test
    @DisplayName("sin traducción ni trabajo, pide la traducción desde el español y avisa que se está generando")
    void requestsTranslationWhenMissing() {
        when(translations.findByProfileAndLanguage(30L, 2)).thenReturn(Optional.empty());
        when(jobs.stateOf(30L, 2)).thenReturn(JobState.NONE);

        ProfileResponse response = useCase.getByUserId(7, "en");

        verify(jobs).request(30L, "es");
        assertThat(response.getTranslationGenerating()).isTrue();
        assertThat(response.getTranslatedBio()).isNull();
    }

    @Test
    @DisplayName("con un trabajo en curso no pide otro; si falló, no muestra el indicador")
    void doesNotDuplicateJobs() {
        when(translations.findByProfileAndLanguage(30L, 2)).thenReturn(Optional.empty());

        when(jobs.stateOf(30L, 2)).thenReturn(JobState.IN_PROGRESS);
        assertThat(useCase.getByUserId(7, "en").getTranslationGenerating()).isTrue();

        when(jobs.stateOf(30L, 2)).thenReturn(JobState.FAILED);
        assertThat(useCase.getByUserId(7, "en").getTranslationGenerating()).isFalse();

        verify(jobs, never()).request(anyLong(), any());
    }

    @Test
    @DisplayName("en español (idioma de origen) no consulta a Localización")
    void spanishIsTheSource() {
        ProfileResponse response = useCase.getByUserId(7, "es");

        assertThat(response.getTranslatedBio()).isNull();
        verify(languages, never()).findIdByCode(any());
        verifyNoInteractions(translations, jobs);
    }
}
