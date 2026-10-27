package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EventTranslationController")
class EventTranslationControllerTest {

    private static final LanguageSummary EN = LanguageSummary.builder().languageId(2).code("en").build();
    private static final LanguageSummary ES = LanguageSummary.builder().languageId(1).code("es").build();
    private static final IllegalStateException DB_DOWN = new IllegalStateException("bd caída");

    private final EventTranslationRepository translations = mock(EventTranslationRepository.class);
    private final EventRepository events = mock(EventRepository.class);
    private final LanguageCatalog languages = mock(LanguageCatalog.class);
    private final EventTranslationController controller = new EventTranslationController(translations, events, languages);

    private static Event event(long id, String name, String description) {
        return Event.builder().eventId(id).eventName(name).description(description).build();
    }

    private static EventTranslation translation(int eventId, String name) {
        return EventTranslation.builder().translationId(eventId * 10).eventId(eventId).languageId(2)
                .translatedName(name).translatedDescription("Live").source(TranslationSource.MANUAL)
                .status(TranslationStatus.PUBLISHED).build();
    }

    @Test
    @DisplayName("listado: idioma desconocido 400, filas con y sin traducción, filtro y error 500")
    void list() {
        when(languages.findByCode("xx")).thenReturn(Optional.empty());
        assertThat(controller.getEventsWithTranslations("XX", "all").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        when(languages.findByCode("en")).thenReturn(Optional.of(EN));
        when(events.findAll()).thenReturn(List.of(event(1, "Concierto", "En vivo"), event(2, "Teatro", null)));
        when(translations.findByEventAndLanguage(1, 2)).thenReturn(Optional.of(translation(1, "Concert")));
        when(translations.findByEventAndLanguage(2, 2)).thenReturn(Optional.empty());

        List<Map<String, Object>> rows = controller.getEventsWithTranslations("en", "all").getBody().getData();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("translatedName", "Concert").containsEntry("translatedDescription", "Live")
                .containsEntry("description", "En vivo").containsEntry("translationStatus", "PUBLISHED");
        assertThat(rows.get(1)).containsEntry("description", "").containsEntry("hasTranslation", false);

        assertThat(controller.getEventsWithTranslations("en", "not-translated").getBody().getData())
                .singleElement().satisfies(r -> assertThat(r).containsEntry("eventId", 2L));

        when(events.findAll()).thenThrow(DB_DOWN);
        assertThat(controller.getEventsWithTranslations("en", "all").getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("alta con nombre y descripción recortados; edición con valores por defecto")
    void save() {
        when(events.findById(1L)).thenReturn(Optional.of(event(1, "Concierto", "En vivo")));
        when(languages.findById(2)).thenReturn(Optional.of(EN));
        when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(translations.findByEventAndLanguage(1, 2)).thenReturn(Optional.empty());
        EventTranslation created = controller.createOrUpdateTranslation(1, 2, Map.of("translatedName", " Concert ",
                "translatedDescription", " Live ", "source", "MACHINE", "status", "DRAFT")).getBody().getData();
        assertThat(created.getTranslatedName()).isEqualTo("Concert");
        assertThat(created.getTranslatedDescription()).isEqualTo("Live");
        assertThat(created.getSource()).isEqualTo(TranslationSource.MACHINE);
        assertThat(created.getStatus()).isEqualTo(TranslationStatus.DRAFT);

        EventTranslation existing = translation(1, "Concert");
        existing.setStatus(TranslationStatus.DRAFT);
        when(translations.findByEventAndLanguage(1, 2)).thenReturn(Optional.of(existing));
        EventTranslation updated = controller.createOrUpdateTranslation(1, 2, Map.of()).getBody().getData();
        assertThat(updated.getTranslationId()).isEqualTo(10);
        assertThat(updated.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("evento o idioma inexistentes, o el español, son 400; un error al guardar es 500")
    void saveRejected() {
        when(events.findById(9L)).thenReturn(Optional.empty());
        when(events.findById(1L)).thenReturn(Optional.of(event(1, "Concierto", null)));
        when(languages.findById(7)).thenReturn(Optional.empty());
        when(languages.findById(1)).thenReturn(Optional.of(ES));
        when(languages.findById(2)).thenReturn(Optional.of(EN));

        assertThat(controller.createOrUpdateTranslation(9, 2, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.createOrUpdateTranslation(1, 7, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.createOrUpdateTranslation(1, 1, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(translations, never()).save(any());

        when(translations.findByEventAndLanguage(1, 2)).thenReturn(Optional.empty());
        when(translations.save(any())).thenThrow(DB_DOWN);
        assertThat(controller.createOrUpdateTranslation(1, 2, Map.of()).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("consulta y borrado de una traducción: existente, inexistente y error")
    void getAndDelete() {
        EventTranslation existing = translation(1, "Concert");
        when(translations.findByEventAndLanguage(1, 2)).thenReturn(Optional.of(existing));
        when(translations.findByEventAndLanguage(3, 2)).thenReturn(Optional.empty());
        when(translations.findByEventAndLanguage(5, 2)).thenThrow(DB_DOWN);

        assertThat(controller.getTranslation(1, 2).getBody().getData()).isSameAs(existing);
        assertThat(controller.getTranslation(3, 2).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // El fallo llega al GlobalExceptionHandler
        assertThatThrownBy(() -> controller.getTranslation(5, 2)).isSameAs(DB_DOWN);

        assertThat(controller.deleteTranslation(1, 2).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(translations).delete(existing);
        assertThat(controller.deleteTranslation(3, 2).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.deleteTranslation(5, 2).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("todas las traducciones de un evento: existente, inexistente 400 y error 500")
    void allForEvent() {
        when(events.findById(1L)).thenReturn(Optional.of(event(1, "Concierto", null)));
        when(events.findById(9L)).thenReturn(Optional.empty());
        when(events.findById(5L)).thenThrow(DB_DOWN);
        when(translations.findByEvent(1)).thenReturn(List.of(translation(1, "Concert")));

        assertThat(controller.getAllTranslationsForEvent(1).getBody().getData()).hasSize(1);
        assertThat(controller.getAllTranslationsForEvent(9).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.getAllTranslationsForEvent(5).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
