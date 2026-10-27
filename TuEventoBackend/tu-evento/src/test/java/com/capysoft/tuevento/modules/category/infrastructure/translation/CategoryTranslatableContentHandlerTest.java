package com.capysoft.tuevento.modules.category.infrastructure.translation;

import com.capysoft.tuevento.modules.category.domain.model.CategoryTranslation;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryRepository;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Guardado y estado de la traducción de una categoría. */
@DisplayName("CategoryTranslatableContentHandler")
class CategoryTranslatableContentHandlerTest {

    private final CategoryTranslationRepository translations = mock(CategoryTranslationRepository.class);
    private final LanguageCatalog languages = mock(LanguageCatalog.class);
    private final CategoryTranslatableContentHandler handler =
            new CategoryTranslatableContentHandler(mock(CategoryRepository.class), translations, languages);

    @BeforeEach
    void languages() {
        when(languages.findByCode("en")).thenReturn(Optional.of(LanguageSummary.builder().languageId(2).code("en").build()));
        when(languages.findByCode("xx")).thenReturn(Optional.empty());
        when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("crea la traducción con nombre y descripción, origen y estado")
    void creates() {
        when(translations.findByCategoryAndLanguage(5, 2)).thenReturn(Optional.empty());

        handler.saveTranslation(5L, "en", Map.of("name", "Music", "description", "Concerts"),
                TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        ArgumentCaptor<CategoryTranslation> saved = ArgumentCaptor.forClass(CategoryTranslation.class);
        verify(translations).save(saved.capture());
        assertThat(saved.getValue()).extracting(CategoryTranslation::getCategoryId, CategoryTranslation::getLanguageId,
                        CategoryTranslation::getTranslatedName, CategoryTranslation::getTranslatedDescription,
                        CategoryTranslation::getSource, CategoryTranslation::getStatus)
                .containsExactly(5, 2, "Music", "Concerts", TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("sin textos útiles o con idioma desconocido no guarda")
    void skips() {
        when(translations.findByCategoryAndLanguage(5, 2))
                .thenReturn(Optional.of(CategoryTranslation.builder().categoryId(5).languageId(2).build()));

        handler.saveTranslation(5L, "en", Map.of("name", " "), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
        handler.saveTranslation(5L, "xx", Map.of("name", "Music"), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        verify(translations, never()).save(any());
    }

    @Test
    @DisplayName("estado de la traducción, o vacío si el idioma no existe")
    void status() {
        when(translations.findByCategoryAndLanguage(5, 2))
                .thenReturn(Optional.of(CategoryTranslation.builder().status(TranslationStatus.DRAFT).build()));

        assertThat(handler.findTranslationStatus(5L, "en")).contains(TranslationStatus.DRAFT);
        assertThat(handler.findTranslationStatus(5L, "xx")).isEmpty();
    }
}
