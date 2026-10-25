package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.SaveEventLayoutRequest;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventLayout;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventLayoutRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatBlock;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatBlockRepository;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regeneración de sillas al guardar el layout: una consulta para validar, una sentencia
 * de borrado por tabla e inserción por lotes, sin operaciones silla por silla.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SaveEventLayoutService — regeneración masiva de sillas")
class SaveEventLayoutServiceTest {

    private static final long EVENT_ID = 5L;
    private static final long OWNER_ID = 9L;
    private static final int SECTION_ID = 70;

    @Mock EventRepository eventRepository;
    @Mock EventLayoutRepository eventLayoutRepository;
    @Mock SeatBlockRepository seatBlockRepository;
    @Mock SeatRepository seatRepository;

    private SaveEventLayoutService service;

    @BeforeEach
    void setUp() {
        service = new SaveEventLayoutService(eventRepository, eventLayoutRepository,
                seatBlockRepository, seatRepository, new ObjectMapper());
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(Event.builder()
                .eventId(EVENT_ID).userId(OWNER_ID).status(EventStatus.DRAFT).build()));
        when(eventLayoutRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(eventLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Layout con una sección lógica partida en sub-elementos (targetSeats, seatsPerRow). */
    private static String layout(int[]... subElements) {
        StringBuilder json = new StringBuilder("{\"elements\":[");
        for (int i = 0; i < subElements.length; i++) {
            if (i > 0) json.append(',');
            json.append("{\"type\":\"section\",\"backendSectionId\":").append(SECTION_ID)
                    .append(",\"seatLayout\":{\"targetSeats\":").append(subElements[i][0])
                    .append(",\"rows\":1,\"seatsPerRow\":").append(subElements[i][1]).append("}}");
        }
        return json.append("]}").toString();
    }

    private void save(String layoutData) {
        service.execute(EVENT_ID, SaveEventLayoutRequest.builder().layoutData(layoutData).build(), OWNER_ID);
    }

    private List<Seat> insertedSeats() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Seat>> captor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).insertAll(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("borra la sección con una sentencia por tabla e inserta todas las sillas en una sola llamada por lotes")
    void regeneratesWithBulkOperations() {
        when(seatBlockRepository.save(any())).thenReturn(SeatBlock.builder().seatBlockId(33).build());

        save(layout(new int[]{3, 2}, new int[]{2, 2}));

        verify(seatRepository).existsByEventSectionIdAndStatusNot(SECTION_ID, SeatStatus.AVAILABLE);
        verify(seatRepository).deleteAllByEventSectionId(SECTION_ID);
        verify(seatBlockRepository).deleteAllByEventSectionId(SECTION_ID);
        verify(seatRepository, never()).findAllByEventSectionId(anyInt());
        verify(seatRepository, never()).deleteById(anyInt());
        verify(seatRepository, never()).save(any());

        List<Seat> seats = insertedSeats();
        // Numeración continua entre sub-elementos, con el seatsPerRow máximo (2)
        assertThat(seats).extracting(Seat::getCode).containsExactly("A1", "A2", "B1", "B2", "C1");
        assertThat(seats).allSatisfy(seat -> {
            assertThat(seat.getSeatBlockId()).isEqualTo(33);
            assertThat(seat.getEventSectionId()).isEqualTo(SECTION_ID);
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        });
    }

    @Test
    @DisplayName("un estadio de 132.000 sillas se inserta en una sola llamada por lotes")
    void largeVenueIsInsertedInOneBulkCall() {
        when(seatBlockRepository.save(any())).thenReturn(SeatBlock.builder().seatBlockId(1).build());

        save(layout(new int[]{132_000, 100}));

        List<Seat> seats = insertedSeats();
        assertThat(seats).hasSize(132_000);
        assertThat(seats.get(0).getCode()).isEqualTo("A1");
        // Fila 1320 → "AXT" (A=1 … Z=26, AA=27…), posición 100
        assertThat(seats.get(seats.size() - 1).getCode()).isEqualTo("AXT100");
    }

    @Test
    @DisplayName("si alguna silla no está disponible, no borra ni inserta nada")
    void rejectsRegenerationWhenSeatsAreTaken() {
        when(seatRepository.existsByEventSectionIdAndStatusNot(SECTION_ID, SeatStatus.AVAILABLE)).thenReturn(true);

        assertThatThrownBy(() -> save(layout(new int[]{10, 5})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reservas o ventas");

        verify(seatRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatBlockRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatRepository, never()).insertAll(any());
    }
}
