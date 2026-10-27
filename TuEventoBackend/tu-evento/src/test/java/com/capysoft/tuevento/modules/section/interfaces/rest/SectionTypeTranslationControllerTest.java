package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.model.SectionTypeTranslation;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeTranslationRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
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

@DisplayName("SectionTypeTranslationController")
class SectionTypeTranslationControllerTest {

    private static final Language EN = Language.builder().languageId(2).code("en").name("English").build();
    private static final Language ES = Language.builder().languageId(1).code("es").name("Español").build();

    private final SectionTypeTranslationRepository translations = mock(SectionTypeTranslationRepository.class);
    private final SectionTypeRepository sectionTypes = mock(SectionTypeRepository.class);
    private final LanguageRepository languages = mock(LanguageRepository.class);
    private final TranslationRequests jobs = mock(TranslationRequests.class);
    private final SectionTypeTranslationController controller =
            new SectionTypeTranslationController(translations, sectionTypes, languages, jobs);

    private static SectionType type(int id, String name) {
        return SectionType.builder().sectionTypeId(id).name(name).active(true).build();
    }

    private static SectionTypeTranslation translation(int typeId, String name) {
        SectionTypeTranslation t = new SectionTypeTranslation();
        t.setTranslationId(typeId * 10);
        t.setSectionTypeId(typeId);
        t.setLanguageId(2);
        t.setTranslatedName(name);
        t.setSource(TranslationSource.MANUAL);
        t.setStatus(TranslationStatus.PUBLISHED);
        return t;
    }

    @Test
    @DisplayName("listado: idioma desconocido 400, filas con traducción y en generación, error 500")
    void list() {
        when(languages.findByCode("xx")).thenReturn(Optional.empty());
        assertThat(controller.getSectionTypesWithTranslations("XX", "all").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        when(languages.findByCode("en")).thenReturn(Optional.of(EN));
        when(sectionTypes.findAll()).thenReturn(List.of(type(1, "Palco"), type(2, "General")));
        when(translations.findBySectionTypeAndLanguage(1L, 2)).thenReturn(Optional.of(translation(1, "Box")));
        when(translations.findBySectionTypeAndLanguage(2L, 2)).thenReturn(Optional.empty());
        when(jobs.jobStatus("section_type", 2, 2)).thenReturn(Optional.of(TranslationJobStatus.PENDING));

        List<Map<String, Object>> rows = controller.getSectionTypesWithTranslations("en", "all").getBody().getData();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("translatedName", "Box").containsEntry("translationStatus", "PUBLISHED")
                .containsEntry("translationGenerating", false);
        assertThat(rows.get(1)).containsEntry("hasTranslation", false).containsEntry("translationGenerating", true);

        assertThat(controller.getSectionTypesWithTranslations("en", "translated").getBody().getData())
                .singleElement().satisfies(r -> assertThat(r).containsEntry("sectionTypeId", 1));

        when(sectionTypes.findAll()).thenThrow(new IllegalStateException("bd caída"));
        assertThat(controller.getSectionTypesWithTranslations("en", "all").getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("alta con valores del cuerpo y edición con valores por defecto")
    void save() {
        when(sectionTypes.findById(1)).thenReturn(Optional.of(type(1, "Palco")));
        when(languages.findById(2)).thenReturn(Optional.of(EN));
        when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(translations.findBySectionTypeAndLanguage(1L, 2)).thenReturn(Optional.empty());
        SectionTypeTranslation created = controller.createOrUpdateTranslation(
                1, 2, Map.of("translatedName", " Box ", "source", "MACHINE", "status", "DRAFT")).getBody().getData();
        assertThat(created.getSectionTypeId()).isEqualTo(1);
        assertThat(created.getTranslatedName()).isEqualTo("Box");
        assertThat(created.getSource()).isEqualTo(TranslationSource.MACHINE);
        assertThat(created.getStatus()).isEqualTo(TranslationStatus.DRAFT);

        SectionTypeTranslation existing = translation(1, "Box");
        existing.setStatus(TranslationStatus.DRAFT);
        when(translations.findBySectionTypeAndLanguage(1L, 2)).thenReturn(Optional.of(existing));
        SectionTypeTranslation updated = controller.createOrUpdateTranslation(1, 2, Map.of()).getBody().getData();
        assertThat(updated.getTranslationId()).isEqualTo(10);
        assertThat(updated.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("tipo o idioma inexistentes, o el español, son 400; un error al guardar es 500")
    void saveRejected() {
        when(sectionTypes.findById(9)).thenReturn(Optional.empty());
        when(sectionTypes.findById(1)).thenReturn(Optional.of(type(1, "Palco")));
        when(languages.findById(7)).thenReturn(Optional.empty());
        when(languages.findById(1)).thenReturn(Optional.of(ES));
        when(languages.findById(2)).thenReturn(Optional.of(EN));

        assertThat(controller.createOrUpdateTranslation(9, 2, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.createOrUpdateTranslation(1, 7, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.createOrUpdateTranslation(1, 1, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(translations, never()).save(any());

        when(translations.findBySectionTypeAndLanguage(1L, 2)).thenReturn(Optional.empty());
        when(translations.save(any())).thenThrow(new IllegalStateException("bd caída"));
        assertThat(controller.createOrUpdateTranslation(1, 2, Map.of()).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("borrado: existente 200, sin traducción 404, error 500")
    void delete() {
        SectionTypeTranslation existing = translation(1, "Box");
        when(translations.findBySectionTypeAndLanguage(1L, 2)).thenReturn(Optional.of(existing));
        when(translations.findBySectionTypeAndLanguage(3L, 2)).thenReturn(Optional.empty());
        when(translations.findBySectionTypeAndLanguage(5L, 2)).thenThrow(new IllegalStateException("bd caída"));

        assertThat(controller.deleteTranslation(1, 2).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(translations).delete(existing);
        assertThat(controller.deleteTranslation(3, 2).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.deleteTranslation(5, 2).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
