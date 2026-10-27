package com.capysoft.tuevento.modules.profile.interfaces.rest;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProfileTranslationController")
class ProfileTranslationControllerTest {

    private static final Language EN = Language.builder().languageId(2).code("en").name("English").build();
    private static final Language ES = Language.builder().languageId(1).code("es").name("Español").build();

    private final ProfileTranslationRepository translations = mock(ProfileTranslationRepository.class);
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final LanguageRepository languages = mock(LanguageRepository.class);
    private final ProfileTranslationController controller =
            new ProfileTranslationController(translations, profiles, languages);

    private static Profile profile(long id, String bio) {
        return Profile.builder().profileId(id).userId((int) id + 100).fullName("Usuario " + id).bio(bio).build();
    }

    private static ProfileTranslation translation(int profileId, String bio, TranslationStatus status) {
        ProfileTranslation t = new ProfileTranslation();
        t.setTranslationId(profileId * 10);
        t.setProfileId(profileId);
        t.setLanguageId(2);
        t.setTranslatedBio(bio);
        t.setSource(TranslationSource.MACHINE);
        t.setStatus(status);
        return t;
    }

    /** Perfiles 1 traducido, 2 igual al original, 3 vacío, 4 en PENDING_REVIEW, 5 borrado. */
    @BeforeEach
    void data() {
        when(languages.findByCode("en")).thenReturn(Optional.of(EN));
        when(translations.findByLanguage(2)).thenReturn(List.of(
                translation(1, "I like music", TranslationStatus.PUBLISHED),
                translation(2, " hola ", TranslationStatus.PUBLISHED),
                translation(3, " ", TranslationStatus.DRAFT),
                translation(4, "Traveller", TranslationStatus.PENDING_REVIEW),
                translation(5, "Gone", TranslationStatus.PUBLISHED)));
        when(profiles.findById(1L)).thenReturn(Optional.of(profile(1, "Me gusta la música")));
        when(profiles.findById(2L)).thenReturn(Optional.of(profile(2, "Hola")));
        when(profiles.findById(3L)).thenReturn(Optional.of(profile(3, "Algo")));
        when(profiles.findById(4L)).thenReturn(Optional.of(profile(4, null)));
        when(profiles.findById(5L)).thenReturn(Optional.empty());
    }

    private List<Object> ids(String filter) {
        return controller.getProfilesWithTranslations("EN", filter).getBody().stream()
                .map(r -> r.get("profileId")).toList();
    }

    @Test
    @DisplayName("idioma desconocido 400 y sin traducir siempre vacío")
    void guards() {
        when(languages.findByCode("xx")).thenReturn(Optional.empty());

        assertThat(controller.getProfilesWithTranslations("xx", "all").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.getProfilesWithTranslations("en", "not-translated").getBody()).isEmpty();
        verify(translations, never()).findByLanguage(any());
    }

    @Test
    @DisplayName("cada filtro; los perfiles borrados no aparecen")
    void filters() {
        assertThat(ids("all")).containsExactly(1L, 2L, 3L, 4L);
        assertThat(ids("otro")).containsExactly(1L, 2L, 3L, 4L);
        assertThat(ids("translated")).containsExactly(1L, 2L, 4L);
        assertThat(ids("needs-review")).containsExactly(2L, 3L, 4L);
    }

    @Test
    @DisplayName("las filas llevan perfil y traducción; la marca de revisión solo con ese filtro")
    void rows() {
        Map<String, Object> first = controller.getProfilesWithTranslations("en", "all").getBody().get(0);
        assertThat(first).containsEntry("userId", 101).containsEntry("fullName", "Usuario 1")
                .containsEntry("bio", "Me gusta la música").containsEntry("translationId", 10)
                .containsEntry("translatedBio", "I like music")
                .containsEntry("translationStatus", TranslationStatus.PUBLISHED)
                .containsEntry("translationSource", TranslationSource.MACHINE).containsEntry("needsReview", false);

        assertThat(controller.getProfilesWithTranslations("en", "all").getBody().get(1))
                .containsEntry("needsReview", false);
        assertThat(controller.getProfilesWithTranslations("en", "needs-review").getBody())
                .allSatisfy(row -> assertThat(row).containsEntry("needsReview", true));
    }

    @Test
    @DisplayName("alta o edición rechaza perfil, idioma, español y biografía vacía")
    void updateRejects() {
        when(profiles.findById(9L)).thenReturn(Optional.empty());
        when(languages.findById(7)).thenReturn(Optional.empty());
        when(languages.findById(1)).thenReturn(Optional.of(ES));
        when(languages.findById(2)).thenReturn(Optional.of(EN));

        assertThat(controller.updateProfileTranslation(9L, 2, Map.of("translatedBio", "x")).getBody())
                .containsEntry("error", "Profile not found with ID: 9");
        assertThat(controller.updateProfileTranslation(1L, 7, Map.of("translatedBio", "x")).getBody())
                .containsEntry("error", "Language not found with ID: 7");
        assertThat(controller.updateProfileTranslation(1L, 1, Map.of("translatedBio", "x")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.updateProfileTranslation(1L, 2, Map.of("translatedBio", " ")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.updateProfileTranslation(1L, 2, Map.of()).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(translations, never()).save(any());
    }

    @Test
    @DisplayName("alta: manual y publicada por defecto; edición: respeta source y status del cuerpo")
    void updateSaves() {
        when(languages.findById(2)).thenReturn(Optional.of(EN));
        when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(translations.findByProfileAndLanguage(2L, 2)).thenReturn(Optional.empty());
        when(translations.findByProfileAndLanguage(1L, 2))
                .thenReturn(Optional.of(translation(1, "I like music", TranslationStatus.PUBLISHED)));

        Map<String, Object> created =
                controller.updateProfileTranslation(2L, 2, Map.of("translatedBio", "  Hello  ")).getBody();
        assertThat(created).containsEntry("profileId", 2).containsEntry("languageId", 2)
                .containsEntry("translatedBio", "Hello").containsEntry("source", TranslationSource.MANUAL)
                .containsEntry("status", TranslationStatus.PUBLISHED).containsEntry("translationId", null);

        Map<String, Object> updated = controller.updateProfileTranslation(1L, 2,
                Map.of("translatedBio", "I love music", "source", "MACHINE", "status", "DRAFT")).getBody();
        assertThat(updated).containsEntry("translationId", 10).containsEntry("translatedBio", "I love music")
                .containsEntry("source", TranslationSource.MACHINE).containsEntry("status", TranslationStatus.DRAFT);
    }

    @Test
    @DisplayName("borrado: existente 204 y sin traducción 404")
    void delete() {
        when(translations.findByProfileAndLanguage(1L, 2))
                .thenReturn(Optional.of(translation(1, "I like music", TranslationStatus.PUBLISHED)));
        when(translations.findByProfileAndLanguage(3L, 2)).thenReturn(Optional.empty());

        assertThat(controller.deleteProfileTranslation(1L, 2).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(translations).delete(any());
        assertThat(controller.deleteProfileTranslation(3L, 2).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
