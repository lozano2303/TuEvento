package com.capysoft.tuevento.modules.category.application.listener;

import com.capysoft.tuevento.modules.category.domain.event.CategoryCreatedEvent;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Crear una categoría pide sus traducciones; un fallo no afecta a la creación.
 */
@DisplayName("CategoryTranslationListener")
class CategoryTranslationListenerTest {

    private final TranslationRequests requests = mock(TranslationRequests.class);
    private final CategoryTranslationListener listener = new CategoryTranslationListener(requests);
    private final CategoryCreatedEvent created = CategoryCreatedEvent.builder().categoryId(5).name("Música").build();

    @Test
    @DisplayName("pide la traducción de la categoría creada")
    void requests() {
        listener.onCategoryCreated(created);

        verify(requests).request("category", 5L);
    }

    @Test
    @DisplayName("un fallo al pedirla no se propaga")
    void failureIsSwallowed() {
        doThrow(new IllegalStateException("bd caída")).when(requests).request("category", 5L);

        assertThatCode(() -> listener.onCategoryCreated(created)).doesNotThrowAnyException();
    }
}
