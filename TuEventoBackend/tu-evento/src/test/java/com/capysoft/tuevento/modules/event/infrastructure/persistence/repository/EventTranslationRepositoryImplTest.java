package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventTranslationEntity;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.mapper.EventTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Adaptador de persistencia de traducciones de eventos con el mapper real y el repositorio JPA simulado.
 */
@DisplayName("EventTranslationRepositoryImpl")
class EventTranslationRepositoryImplTest {

    private final EventTranslationJpaRepository jpa = mock(EventTranslationJpaRepository.class);
    private final EventTranslationMapper mapper = new EventTranslationMapper();
    private final EventTranslationRepositoryImpl repository = new EventTranslationRepositoryImpl(jpa, mapper);

    private static EventTranslationEntity entity(Integer id) {
        return EventTranslationEntity.builder().translationId(id).eventId(7).languageId(2)
                .translatedName("Concert").translatedDescription("Live").source(TranslationSource.MACHINE)
                .status(TranslationStatus.PUBLISHED).build();
    }

    private static EventTranslation domain(Integer id) {
        return EventTranslation.builder().translationId(id).eventId(7).languageId(2).translatedName("Show")
                .translatedDescription("Desc").source(TranslationSource.MANUAL).status(TranslationStatus.DRAFT).build();
    }

    @Test
    @DisplayName("busca por evento e idioma y todas las de un evento")
    void finds() {
        when(jpa.findByEventIdAndLanguageId(7, 2)).thenReturn(Optional.of(entity(5)));
        when(jpa.findByEventId(7)).thenReturn(List.of(entity(5), entity(6)));

        assertThat(repository.findByEventAndLanguage(7, 2)).get()
                .satisfies(t -> assertThat(t.getTranslatedDescription()).isEqualTo("Live"));
        assertThat(repository.findByEvent(7)).extracting(EventTranslation::getTranslationId).containsExactly(5, 6);
    }

    @Test
    @DisplayName("sin id crea; con id actualiza la existente; con un id que no existe falla")
    void saves() {
        when(jpa.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(repository.save(domain(null)).getTranslatedName()).isEqualTo("Show");
        verify(jpa, never()).findById(any());

        EventTranslationEntity existing = entity(5);
        when(jpa.findById(5)).thenReturn(Optional.of(existing));
        EventTranslation updated = repository.save(domain(5));
        assertThat(updated.getTranslatedName()).isEqualTo("Show");
        assertThat(existing.getStatus()).isEqualTo(TranslationStatus.DRAFT);

        when(jpa.findById(9)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> repository.save(domain(9))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("borra por id, no hace nada sin id, y borra todas las de un evento")
    void deletes() {
        repository.delete(domain(5));
        repository.delete(domain(null));
        repository.deleteByEvent(7);

        verify(jpa).deleteById(5);
        verify(jpa, never()).deleteById(null);
        verify(jpa).deleteByEventId(7);
    }

    @Test
    @DisplayName("el mapper tolera nulos")
    void mapperNulls() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
        EventTranslationEntity untouched = entity(1);
        mapper.updateEntity(untouched, null);
        mapper.updateEntity(null, domain(1));
        assertThat(untouched.getTranslatedName()).isEqualTo("Concert");
    }
}
