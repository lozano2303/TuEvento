package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.CreateEventRequest;
import com.capysoft.tuevento.modules.event.application.dto.request.UpdateEventRequest;
import com.capysoft.tuevento.modules.event.application.port.out.SiteLookupPort;
import com.capysoft.tuevento.modules.event.application.port.out.SiteSummary;
import com.capysoft.tuevento.modules.event.application.validator.EventDateValidator;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/**
 * Validación de la sede al crear y editar eventos a través de {@link SiteLookupPort}: sede
 * inexistente, asientos por encima de la capacidad y servicio de geolocalización caído.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Validación de sede en eventos")
class EventSiteValidationTest {

    private static final SiteSummary SMALL_SITE = new SiteSummary(7, "Teatro", 100);

    @Nested
    @DisplayName("al crear")
    class Create {

        @Mock EventRepository eventRepository;
        @Mock SiteLookupPort siteLookupPort;
        @Mock EventDateValidator eventDateValidator;
        @InjectMocks CreateEventService service;

        private CreateEventRequest request(int seats) {
            return CreateEventRequest.builder().eventName("Concierto").siteId(7L)
                    .availableSeats(seats).build();
        }

        @Test
        @DisplayName("una sede inexistente da SITE_NOT_FOUND")
        void siteNotFound() {
            when(siteLookupPort.getSite(7)).thenThrow(new NotFoundException("SITE_NOT_FOUND", "no"));

            assertThatThrownBy(() -> service.execute(request(10), 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("code", "SITE_NOT_FOUND");
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("más asientos que la capacidad da SEATS_EXCEED_CAPACITY")
        void seatsExceedCapacity() {
            when(siteLookupPort.getSite(7)).thenReturn(SMALL_SITE);

            assertThatThrownBy(() -> service.execute(request(101), 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "SEATS_EXCEED_CAPACITY")
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("si geolocalización no responde, el 503 llega tal cual")
        void serviceUnavailable() {
            when(siteLookupPort.getSite(7)).thenThrow(
                    new ServiceUnavailableException("GEOLOCATION_UNAVAILABLE", "down"));

            assertThatThrownBy(() -> service.execute(request(10), 1L))
                    .isInstanceOf(ServiceUnavailableException.class);
        }
    }

    @Nested
    @DisplayName("al editar")
    class Update {

        @Mock EventRepository eventRepository;
        @Mock SiteLookupPort siteLookupPort;
        @Mock EventDateValidator eventDateValidator;
        @InjectMocks UpdateEventService service;

        private void draftEvent() {
            Event event = Event.builder().eventId(5L).userId(1L).siteId(7L)
                    .status(EventStatus.DRAFT).availableSeats(10).eventName("Concierto").build();
            when(eventRepository.findById(5L)).thenReturn(Optional.of(event));
        }

        @Test
        @DisplayName("una sede inexistente da SITE_NOT_FOUND")
        void siteNotFound() {
            draftEvent();
            when(siteLookupPort.getSite(7)).thenThrow(new NotFoundException("SITE_NOT_FOUND", "no"));

            assertThatThrownBy(() -> service.execute(5L, new UpdateEventRequest(), 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasFieldOrPropertyWithValue("code", "SITE_NOT_FOUND");
        }

        @Test
        @DisplayName("más asientos que la capacidad da SEATS_EXCEED_CAPACITY")
        void seatsExceedCapacity() {
            draftEvent();
            when(siteLookupPort.getSite(7)).thenReturn(SMALL_SITE);
            UpdateEventRequest request = UpdateEventRequest.builder().availableSeats(500).build();

            assertThatThrownBy(() -> service.execute(5L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "SEATS_EXCEED_CAPACITY");
            verify(eventRepository, never()).save(any());
        }
    }
}
