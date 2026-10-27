package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingTranslationRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@DisplayName("EventRatingTranslationController")
class EventRatingTranslationControllerTest {

    private static final Language EN = Language.builder().languageId(2).code("en").name("English").build();

    private final EventRatingTranslationRepository translations = mock(EventRatingTranslationRepository.class);
    private final EventRatingRepository ratings = mock(EventRatingRepository.class);
    private final LanguageRepository languages = mock(LanguageRepository.class);
    private final TranslationRequests jobs = mock(TranslationRequests.class);
    private final EventRepository events = mock(EventRepository.class);
    private final EventRatingTranslationController controller =
            new EventRatingTranslationController(translations, ratings, languages, jobs, events);

    private static EventRating comment(long id, String text, Long parentId) {
        return EventRating.builder().ratingId(id).eventId(9L).comment(text).rating(5).parentRatingId(parentId).build();
    }

    private static EventRatingTranslation translation(int ratingId, String text, TranslationStatus status) {
        EventRatingTranslation t = new EventRatingTranslation();
        t.setTranslationId(ratingId * 10);
        t.setRatingId(ratingId);
        t.setLanguageId(2);
        t.setTranslatedComment(text);
        t.setStatus(status);
        return t;
    }

    /** Comentarios 1 traducido, 2 sin traducción y generándose, 3 sin traducción, 4 igual al original, 5 respuesta. */
    @BeforeEach
    void comments() {
        when(languages.findByCode("en")).thenReturn(Optional.of(EN));
        when(events.findById(9L)).thenReturn(Optional.of(Event.builder().eventId(9L).eventName("Concierto").build()));
        when(ratings.findByEventIdOrderByCreatedAtDesc(9L)).thenReturn(List.of(
                comment(1, "Genial", null), comment(2, "Bueno", null), comment(3, "Malo", null),
                comment(4, "OK", null), comment(5, "Respuesta", 1L)));
        when(translations.findByRatingAndLanguage(1, 2))
                .thenReturn(Optional.of(translation(1, "Great", TranslationStatus.PUBLISHED)));
        when(translations.findByRatingAndLanguage(2, 2)).thenReturn(Optional.empty());
        when(translations.findByRatingAndLanguage(3, 2)).thenReturn(Optional.empty());
        when(translations.findByRatingAndLanguage(4, 2))
                .thenReturn(Optional.of(translation(4, " ok ", TranslationStatus.PUBLISHED)));
        when(jobs.jobStatus("event_rating", 2, 2))
                .thenReturn(Optional.of(TranslationJobStatus.PENDING));
    }

    private List<Object> ids(String filter) {
        return controller.getAllCommentTranslations("en", 9L, filter).getBody().stream()
                .map(r -> r.get("ratingId")).toList();
    }

    @Test
    @DisplayName("idioma desconocido 400 y sin evento devuelve lista vacía")
    void guards() {
        when(languages.findByCode("xx")).thenReturn(Optional.empty());

        assertThat(controller.getAllCommentTranslations("xx", 9L, "all").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.getAllCommentTranslations("en", null, "all").getBody()).isEmpty();
    }

    @Test
    @DisplayName("cada filtro, sin respuestas; sin traducir excluye la que se está generando")
    void filters() {
        assertThat(ids("all")).containsExactly(1L, 2L, 3L, 4L);
        assertThat(ids("otro")).containsExactly(1L, 2L, 3L, 4L);
        assertThat(ids("not-translated")).containsExactly(3L);
        assertThat(ids("translated")).containsExactly(1L, 4L);
        assertThat(ids("needs-review")).containsExactly(2L, 3L, 4L);
    }

    @Test
    @DisplayName("las filas llevan evento, traducción, generación y la marca de revisión solo en ese filtro")
    void rows() {
        Map<String, Object> first = controller.getAllCommentTranslations("en", 9L, "all").getBody().get(0);
        assertThat(first).containsEntry("eventName", "Concierto").containsEntry("translatedComment", "Great")
                .containsEntry("translationStatus", TranslationStatus.PUBLISHED).containsEntry("needsReview", false);

        List<Map<String, Object>> review = controller.getAllCommentTranslations("en", 9L, "needs-review").getBody();
        assertThat(review.get(0)).containsEntry("ratingId", 2L).containsEntry("translationGenerating", true)
                .containsEntry("needsReview", true).containsEntry("translationId", null);
        assertThat(controller.getAllCommentTranslations("en", 9L, "all").getBody().get(1))
                .containsEntry("needsReview", false);
    }

    @Test
    @DisplayName("una traducción en PENDING_REVIEW entra en revisión; evento borrado se muestra como desconocido")
    void pendingReviewAndUnknownEvent() {
        when(translations.findByRatingAndLanguage(1, 2))
                .thenReturn(Optional.of(translation(1, "Great", TranslationStatus.PENDING_REVIEW)));
        when(events.findById(9L)).thenReturn(Optional.empty());

        assertThat(ids("needs-review")).contains(1L);
        assertThat(controller.getAllCommentTranslations("en", 9L, "all").getBody().get(0))
                .containsEntry("eventName", "Evento desconocido");
    }

    @Test
    @DisplayName("alta o edición: comentario obligatorio, PUBLISHED por defecto y el estado del cuerpo")
    void update() {
        when(translations.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(controller.updateCommentTranslation(3L, 2, Map.of("translatedComment", " ")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(translations, never()).save(any());

        ResponseEntity<Map<String, Object>> created =
                controller.updateCommentTranslation(3L, 2, Map.of("translatedComment", "Bad"));
        assertThat(created.getBody()).containsEntry("ratingId", 3).containsEntry("translatedComment", "Bad")
                .containsEntry("status", TranslationStatus.PUBLISHED);

        Map<String, Object> updated = controller.updateCommentTranslation(1L, 2,
                Map.of("translatedComment", "Awesome", "status", "DRAFT")).getBody();
        assertThat(updated).containsEntry("translationId", 10).containsEntry("status", TranslationStatus.DRAFT);
    }

    @Test
    @DisplayName("borrado: existente 204 y sin traducción 404")
    void delete() {
        assertThat(controller.deleteCommentTranslation(1L, 2).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(translations).delete(any());
        assertThat(controller.deleteCommentTranslation(3L, 2).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
