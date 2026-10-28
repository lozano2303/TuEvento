package com.capysoft.tuevento.modules.section.application.listener;

import com.capysoft.tuevento.modules.section.domain.event.SectionTypeCreatedEvent;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Crear un tipo de sección pide sus traducciones; un fallo no afecta a la creación.
 */
@DisplayName("SectionTypeTranslationListener")
class SectionTypeTranslationListenerTest {

    private final TranslationRequests requests = mock(TranslationRequests.class);
    private final SectionTypeTranslationListener listener = new SectionTypeTranslationListener(requests);
    private final SectionTypeCreatedEvent created = SectionTypeCreatedEvent.builder().sectionTypeId(3).name("Palco").build();

    @Test
    @DisplayName("pide la traducción del tipo creado")
    void requests() {
        listener.onSectionTypeCreated(created);

        verify(requests).request("section_type", 3L);
    }

    @Test
    @DisplayName("un fallo al pedirla no se propaga")
    void failureIsSwallowed() {
        doThrow(new IllegalStateException("bd caída")).when(requests).request("section_type", 3L);

        assertThatCode(() -> listener.onSectionTypeCreated(created)).doesNotThrowAnyException();
    }
}
