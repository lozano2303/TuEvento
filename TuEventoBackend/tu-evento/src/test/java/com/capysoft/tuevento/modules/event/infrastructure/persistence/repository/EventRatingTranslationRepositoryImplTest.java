package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventRatingTranslation;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventRatingTranslationEntity;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.mapper.EventRatingTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adaptador de persistencia de traducciones de comentarios con el mapper real y el repositorio JPA simulado.
 */
@DisplayName("EventRatingTranslationRepositoryImpl")
class EventRatingTranslationRepositoryImplTest {

    private final EventRatingTranslationJpaRepository jpa = mock(EventRatingTranslationJpaRepository.class);
    private final EventRatingTranslationMapper mapper = new EventRatingTranslationMapper();
    private final EventRatingTranslationRepositoryImpl repository = new EventRatingTranslationRepositoryImpl(jpa, mapper);

    private static EventRatingTranslationEntity entity(int ratingId, int languageId) {
        return EventRatingTranslationEntity.builder().translationId(ratingId * 10).ratingId(ratingId)
                .languageId(languageId).translatedComment("Great").status(TranslationStatus.PUBLISHED).build();
    }

    @Test
    @DisplayName("busca por comentario e idioma y por varios comentarios en un estado")
    void finds() {
        when(jpa.findByRatingIdAndLanguageId(4, 2)).thenReturn(Optional.of(entity(4, 2)));
        when(jpa.findByRatingIdInAndStatus(List.of(4, 5), TranslationStatus.PUBLISHED))
                .thenReturn(List.of(entity(4, 2), entity(5, 3)));

        EventRatingTranslation found = repository.findByRatingAndLanguage(4, 2).orElseThrow();
        assertThat(found.getTranslatedComment()).isEqualTo("Great");
        assertThat(found.getStatus()).isEqualTo(TranslationStatus.PUBLISHED);
        assertThat(repository.findByRatingAndStatus(List.of(4, 5), TranslationStatus.PUBLISHED))
                .extracting(EventRatingTranslation::getRatingId).containsExactly(4, 5);
    }

    @Test
    @DisplayName("sin comentarios no consulta la base")
    void emptyIds() {
        assertThat(repository.findByRatingAndStatus(List.of(), TranslationStatus.PUBLISHED)).isEmpty();
        verify(jpa, never()).findByRatingIdInAndStatus(anyCollection(), any());
    }

    @Test
    @DisplayName("guarda, borra una y borra todas las de un comentario")
    void writes() {
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        EventRatingTranslation translation = mapper.toDomain(entity(4, 2));
        translation.setTranslatedComment("Excellent");

        assertThat(repository.save(translation).getTranslatedComment()).isEqualTo("Excellent");
        repository.delete(translation);
        verify(jpa).delete(any(EventRatingTranslationEntity.class));

        List<EventRatingTranslationEntity> all = List.of(entity(4, 2), entity(4, 3));
        when(jpa.findByRatingId(4)).thenReturn(all);
        repository.deleteByRating(4);
        verify(jpa).deleteAll(all);
    }

    @Test
    @DisplayName("el mapper tolera nulos y la igualdad es por comentario e idioma")
    void mapperAndEquality() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();

        EventRatingTranslation a = mapper.toDomain(entity(4, 2));
        EventRatingTranslation b = mapper.toDomain(entity(4, 2));
        b.setTranslatedComment("otro texto");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(mapper.toDomain(entity(4, 3)));
    }
}
