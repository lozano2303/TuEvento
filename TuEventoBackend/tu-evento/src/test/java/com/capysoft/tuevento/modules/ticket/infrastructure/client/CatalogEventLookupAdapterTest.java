package com.capysoft.tuevento.modules.ticket.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort.EventRef;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogEventLookupAdapter — eventos de Catálogo para Comercio")
class CatalogEventLookupAdapterTest {

    @Mock EventRepository eventRepository;
    @InjectMocks CatalogEventLookupAdapter adapter;

    @Test
    @DisplayName("copia dueño, nombre y estado del evento")
    void mapsEvent() {
        when(eventRepository.findById(5L)).thenReturn(Optional.of(Event.builder()
                .eventId(5L).userId(30L).eventName("Concierto").status(EventStatus.PUBLISHED).build()));

        EventRef ref = adapter.find(5L).orElseThrow();

        assertThat(ref).isEqualTo(new EventRef(5L, 30L, "Concierto", "PUBLISHED"));
        assertThat(ref.isPublished()).isTrue();
    }

    @Test
    @DisplayName("un evento sin estado no cuenta como publicado")
    void eventWithoutStatus() {
        when(eventRepository.findById(5L)).thenReturn(Optional.of(Event.builder().eventId(5L).userId(30L).build()));

        EventRef ref = adapter.find(5L).orElseThrow();

        assertThat(ref.status()).isNull();
        assertThat(ref.isPublished()).isFalse();
    }

    @Test
    @DisplayName("un evento inexistente devuelve vacío")
    void missingEvent() {
        when(eventRepository.findById(5L)).thenReturn(Optional.empty());

        assertThat(adapter.find(5L)).isEmpty();
    }

    @Test
    @DisplayName("findAll devuelve por id los que existen, sin repetir lecturas")
    void findAllSkipsMissing() {
        when(eventRepository.findById(5L)).thenReturn(Optional.of(Event.builder()
                .eventId(5L).userId(30L).eventName("Concierto").status(EventStatus.PUBLISHED).build()));
        when(eventRepository.findById(6L)).thenReturn(Optional.empty());

        assertThat(adapter.findAll(List.of(5L, 5L, 6L))).containsOnlyKeys(5L);
        verify(eventRepository).findById(5L);
    }

    @Test
    @DisplayName("findByOwner devuelve los eventos del organizador")
    void findByOwner() {
        when(eventRepository.findByUserId(30L)).thenReturn(List.of(Event.builder()
                .eventId(5L).userId(30L).eventName("Concierto").status(EventStatus.DRAFT).build()));

        assertThat(adapter.findByOwner(30L)).containsExactly(new EventRef(5L, 30L, "Concierto", "DRAFT"));
    }
}
