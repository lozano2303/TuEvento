package com.capysoft.tuevento.modules.category.interfaces.rest;

import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryRepository;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CategoryTranslationController")
class CategoryTranslationControllerTest {

    private static final Language EN = Language.builder().languageId(2).code("en").name("English").build();
    private static final Language ES = Language.builder().languageId(1).code("es").name("Español").build();

    private final CategoryTranslationRepository translations = mock(CategoryTranslationRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final LanguageRepository languages = mock(LanguageRepository.class);
    private final TranslationJobRepository jobs = mock(TranslationJobRepository.class);
    private final CategoryTranslationController controller =
            new CategoryTranslationController(translations, categories, languages, jobs);

    private static Category category(int id, String name) {
        return Category.builder().categoryId(id).name(name).active(true).dadId(null).build();
    }

    private static CategoryTranslation translation(int categoryId, String name) {
        return CategoryTranslation.builder().translationId(categoryId * 10).categoryId(categoryId).languageId(2)
                .translatedName(name).source(TranslationSource.MANUAL).status(TranslationStatus.PUBLISHED).build();
    }

    @Nested
    @DisplayName("listado por idioma")
    class List_ {

        @Test
        @DisplayName("un idioma desconocido es 400")
        void unknownLanguage() {
            when(languages.findByCode("xx")).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse<List<Map<String, Object>>>> response =
                    controller.getCategoriesWithTranslations("XX", "all");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("lista traducidas y sin traducir, y marca la que se está generando")
        void listsAll() {
            when(languages.findByCode("en")).thenReturn(Optional.of(EN));
            when(categories.findAll()).thenReturn(List.of(category(1, "Música"), category(2, "Teatro")));
            when(translations.findByCategoryAndLanguage(1, 2)).thenReturn(Optional.of(translation(1, "Music")));
            when(translations.findByCategoryAndLanguage(2, 2)).thenReturn(Optional.empty());
            when(jobs.findByEntityAndTargetLanguage("category", 2, 2)).thenReturn(Optional.of(
                    TranslationJob.builder().status(TranslationJobStatus.PROCESSING).build()));

            List<Map<String, Object>> rows = controller.getCategoriesWithTranslations("en", "all").getBody().getData();

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0)).containsEntry("translatedName", "Music").containsEntry("hasTranslation", true)
                    .containsEntry("translationStatus", "PUBLISHED").containsEntry("translationGenerating", false);
            assertThat(rows.get(1)).containsEntry("translatedName", null).containsEntry("hasTranslation", false)
                    .containsEntry("translationGenerating", true);
        }

        @Test
        @DisplayName("needs-review deja solo la que falta o es igual al original")
        void needsReview() {
            when(languages.findByCode("en")).thenReturn(Optional.of(EN));
            when(categories.findAll()).thenReturn(List.of(category(1, "Rock"), category(2, "Teatro")));
            when(translations.findByCategoryAndLanguage(1, 2)).thenReturn(Optional.of(translation(1, "rock")));
            when(translations.findByCategoryAndLanguage(2, 2)).thenReturn(Optional.of(translation(2, "Theater")));

            List<Map<String, Object>> rows =
                    controller.getCategoriesWithTranslations("en", "needs-review").getBody().getData();

            assertThat(rows).singleElement().satisfies(r -> assertThat(r)
                    .containsEntry("categoryId", 1).containsEntry("needsReview", true));
        }

        @Test
        @DisplayName("un error inesperado es 500")
        void unexpectedError() {
            when(languages.findByCode("en")).thenThrow(new IllegalStateException("bd caída"));

            assertThat(controller.getCategoriesWithTranslations("en", "all").getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("alta o edición")
    class Save {

        @Test
        @DisplayName("crea la traducción con el nombre recortado y los valores del cuerpo")
        void creates() {
            when(categories.findById(1)).thenReturn(Optional.of(category(1, "Música")));
            when(languages.findById(2)).thenReturn(Optional.of(EN));
            when(translations.findByCategoryAndLanguage(1, 2)).thenReturn(Optional.empty());
            when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ResponseEntity<ApiResponse<CategoryTranslation>> response = controller.createOrUpdateTranslation(
                    1, 2, Map.of("translatedName", "  Music ", "source", "MACHINE", "status", "DRAFT"));

            CategoryTranslation saved = response.getBody().getData();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(saved.getCategoryId()).isEqualTo(1);
            assertThat(saved.getTranslatedName()).isEqualTo("Music");
            assertThat(saved.getSource()).isEqualTo(TranslationSource.MACHINE);
            assertThat(saved.getStatus()).isEqualTo(TranslationStatus.DRAFT);
        }

        @Test
        @DisplayName("edita la existente; sin source ni status queda manual y publicada")
        void updatesWithDefaults() {
            CategoryTranslation existing = translation(1, "Music");
            existing.setStatus(TranslationStatus.DRAFT);
            when(categories.findById(1)).thenReturn(Optional.of(category(1, "Música")));
            when(languages.findById(2)).thenReturn(Optional.of(EN));
            when(translations.findByCategoryAndLanguage(1, 2)).thenReturn(Optional.of(existing));
            when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CategoryTranslation saved = controller.createOrUpdateTranslation(1, 2, Map.of()).getBody().getData();

            assertThat(saved.getTranslationId()).isEqualTo(10);
            assertThat(saved.getTranslatedName()).isEqualTo("Music");
            assertThat(saved.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
        }

        @Test
        @DisplayName("categoría o idioma inexistentes, o el español, son 400 y no guardan")
        void rejects() {
            when(categories.findById(9)).thenReturn(Optional.empty());
            when(categories.findById(1)).thenReturn(Optional.of(category(1, "Música")));
            when(languages.findById(7)).thenReturn(Optional.empty());
            when(languages.findById(1)).thenReturn(Optional.of(ES));

            assertThat(controller.createOrUpdateTranslation(9, 2, Map.of()).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(controller.createOrUpdateTranslation(1, 7, Map.of()).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(controller.createOrUpdateTranslation(1, 1, Map.of()).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(translations, never()).save(any());
        }

        @Test
        @DisplayName("un error al guardar es 500")
        void saveError() {
            when(categories.findById(1)).thenReturn(Optional.of(category(1, "Música")));
            when(languages.findById(2)).thenReturn(Optional.of(EN));
            when(translations.findByCategoryAndLanguage(1, 2)).thenReturn(Optional.empty());
            when(translations.save(any())).thenThrow(new IllegalStateException("bd caída"));

            assertThat(controller.createOrUpdateTranslation(1, 2, Map.of()).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("borrado")
    class Delete {

        @Test
        @DisplayName("borra la existente; si no hay traducción es 404")
        void deletes() {
            CategoryTranslation existing = translation(1, "Music");
            when(translations.findByCategoryAndLanguage(1, 2)).thenReturn(Optional.of(existing));
            when(translations.findByCategoryAndLanguage(3, 2)).thenReturn(Optional.empty());

            assertThat(controller.deleteTranslation(1, 2).getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(translations).delete(existing);
            assertThat(controller.deleteTranslation(3, 2).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("un error al borrar es 500")
        void deleteError() {
            when(translations.findByCategoryAndLanguage(1, 2)).thenThrow(new IllegalStateException("bd caída"));

            assertThat(controller.deleteTranslation(1, 2).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
