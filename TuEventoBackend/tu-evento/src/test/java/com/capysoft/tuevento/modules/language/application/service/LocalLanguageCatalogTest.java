package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Los dominios buscan idiomas por código o id y reciben solo id y código.
 */
@DisplayName("LocalLanguageCatalog")
class LocalLanguageCatalogTest {

    private final LanguageRepository repository = mock(LanguageRepository.class);
    private final LocalLanguageCatalog catalog = new LocalLanguageCatalog(repository);

    @Test
    @DisplayName("busca por código y por id; lo que no existe queda vacío")
    void finds() {
        Language english = Language.builder().languageId(2).code("en").name("English").build();
        when(repository.findByCode("en")).thenReturn(Optional.of(english));
        when(repository.findById(2)).thenReturn(Optional.of(english));
        when(repository.findByCode("xx")).thenReturn(Optional.empty());

        LanguageSummary expected = LanguageSummary.builder().languageId(2).code("en").build();
        assertThat(catalog.findByCode("en")).contains(expected);
        assertThat(catalog.findById(2)).contains(expected);
        assertThat(catalog.findByCode("xx")).isEmpty();
    }
}
