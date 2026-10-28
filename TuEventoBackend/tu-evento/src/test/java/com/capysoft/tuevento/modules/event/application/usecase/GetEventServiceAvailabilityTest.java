package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
import com.capysoft.tuevento.modules.event.application.port.out.SiteLookupPort;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventTranslationRepository;
import com.capysoft.tuevento.modules.seat.application.usecase.SeatAvailabilityService;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** El listado público y el detalle muestran las sillas disponibles del mapa, no el valor guardado. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GetEventService — sillas disponibles en vivo")
class GetEventServiceAvailabilityTest {

    @Mock EventRepository eventRepository;
    @Mock CategoryEventUseCase categoryEventUseCase;
    @Mock SiteLookupPort siteLookupPort;
    @Mock EventMediaRepository eventMediaRepository;
    @Mock TicketRepository ticketRepository;
    @Mock SeatAvailabilityService seatAvailabilityService;
    @Mock EventTranslationRepository eventTranslationRepository;
    @Mock LanguageCatalog languageCatalog;
    @Mock TranslationRequests translationRequests;
    @InjectMocks GetEventService service;

    private static final Event EVENT = Event.builder().eventId(4L).userId(7L).siteId(1L)
            .eventName("Concierto").status(EventStatus.PUBLISHED).availableSeats(200).build();

    @Test
    @DisplayName("el listado público usa el conteo en vivo")
    void publishedListing() {
        when(eventRepository.findAllPublished()).thenReturn(List.of(EVENT));
        when(seatAvailabilityService.availableInEvent(4L, 200)).thenReturn(197);

        assertThat(service.getPublishedEvents().get(0).getAvailableSeats()).isEqualTo(197);
    }

    @Test
    @DisplayName("el detalle usa el conteo en vivo")
    void detail() {
        when(eventRepository.findById(4L)).thenReturn(Optional.of(EVENT));
        when(seatAvailabilityService.availableInEvent(4L, 200)).thenReturn(150);

        assertThat(service.findById(4L).getAvailableSeats()).isEqualTo(150);
    }
}
