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
import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatBlockRepository;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.EnumSet;
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
    @Mock EventSectionRepository eventSectionRepository;

    private SaveEventLayoutService service;

    @BeforeEach
    void setUp() {
        service = new SaveEventLayoutService(eventRepository, eventLayoutRepository,
                seatBlockRepository, seatRepository, eventSectionRepository, new ObjectMapper());
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(Event.builder()
                .eventId(EVENT_ID).userId(OWNER_ID).status(EventStatus.DRAFT).build()));
        when(eventLayoutRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(eventLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventSectionRepository.findAllByEventId((int) EVENT_ID))
                .thenReturn(List.of(EventSection.builder().eventSectionId(SECTION_ID).eventId((int) EVENT_ID)
                        .sectionTypeId(3).capacity(1).availableSeats(1).price(new BigDecimal("50000")).isActive(true)
                        .build()));
    }

    private static final EnumSet<SeatStatus> TAKEN = EnumSet.of(SeatStatus.RESERVED, SeatStatus.SOLD);

    /** Layout con una sección lógica partida en sub-elementos (targetSeats, seatsPerRow). */
    private static String layout(int[]... subElements) {
        return layoutForSection(SECTION_ID, subElements);
    }

    private static String layoutForSection(int sectionId, int[]... subElements) {
        return layoutWithCourtesy(sectionId, 0, subElements);
    }

    /** Igual que layoutForSection, con las cortesías de la sección repetidas en cada parte (como el editor). */
    private static String layoutWithCourtesy(int sectionId, int courtesySeats, int[]... subElements) {
        StringBuilder json = new StringBuilder("{\"elements\":[");
        for (int i = 0; i < subElements.length; i++) {
            if (i > 0) json.append(',');
            json.append("{\"type\":\"section\",\"backendSectionId\":").append(sectionId)
                    .append(",\"courtesySeats\":").append(courtesySeats)
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

        verify(seatRepository).existsByEventSectionIdAndStatusIn(SECTION_ID, TAKEN);
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
        when(seatRepository.existsByEventSectionIdAndStatusIn(SECTION_ID, TAKEN)).thenReturn(true);

        assertThatThrownBy(() -> save(layout(new int[]{10, 5})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reservas o ventas");

        verify(seatRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatBlockRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatRepository, never()).insertAll(any());
    }

    @Test
    @DisplayName("las últimas sillas de la sección quedan como cortesía y la sección vende el resto")
    void marksTheLastSeatsAsCourtesy() {
        when(seatBlockRepository.save(any())).thenReturn(SeatBlock.builder().seatBlockId(33).build());

        save(layoutWithCourtesy(SECTION_ID, 2, new int[]{3, 2}, new int[]{2, 2}));

        List<Seat> seats = insertedSeats();
        assertThat(seats).extracting(Seat::getStatus).containsExactly(
                SeatStatus.AVAILABLE, SeatStatus.AVAILABLE, SeatStatus.AVAILABLE, SeatStatus.COURTESY, SeatStatus.COURTESY);
        assertThat(seats).extracting(Seat::getType).containsExactly(
                SeatType.REGULAR, SeatType.REGULAR, SeatType.REGULAR, SeatType.COURTESY, SeatType.COURTESY);

        ArgumentCaptor<EventSection> section = ArgumentCaptor.forClass(EventSection.class);
        verify(eventSectionRepository).save(section.capture());
        assertThat(section.getValue().getCapacity()).isEqualTo(5);
        assertThat(section.getValue().getAvailableSeats()).isEqualTo(3);
        assertThat(section.getValue().getPrice()).isEqualByComparingTo("50000");
        assertThat(section.getValue().getSectionTypeId()).isEqualTo(3);
    }

    @Test
    @DisplayName("una sección con cortesías se puede regenerar: solo bloquean las reservas y las ventas")
    void courtesySeatsDoNotBlockRegeneration() {
        when(seatBlockRepository.save(any())).thenReturn(SeatBlock.builder().seatBlockId(33).build());

        save(layoutWithCourtesy(SECTION_ID, 5, new int[]{5, 5}));

        verify(seatRepository).existsByEventSectionIdAndStatusIn(SECTION_ID, TAKEN);
        assertThat(insertedSeats()).allSatisfy(seat -> assertThat(seat.getStatus()).isEqualTo(SeatStatus.COURTESY));
    }

    @Test
    @DisplayName("rechaza más cortesías que sillas o cortesías negativas sin tocar nada")
    void rejectsInvalidCourtesySeats() {
        assertThatThrownBy(() -> save(layoutWithCourtesy(SECTION_ID, 6, new int[]{3, 3}, new int[]{2, 2})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cortesía");
        assertThatThrownBy(() -> save(layoutWithCourtesy(SECTION_ID, -1, new int[]{3, 3})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cortesía");

        verify(seatRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatRepository, never()).insertAll(any());
        verify(eventSectionRepository, never()).save(any());
    }

    private void previousLayout(String layoutData) {
        when(eventLayoutRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(
                EventLayout.builder().eventLayoutId(1L).eventId(EVENT_ID).layoutData(layoutData).build()));
    }

    @Test
    @DisplayName("una sección cuyas sillas no cambiaron no se borra ni se vuelve a insertar")
    void skipsUnchangedSections() {
        String layout = layoutWithCourtesy(SECTION_ID, 2, new int[]{3, 2}, new int[]{2, 2});
        previousLayout(layout);
        when(seatRepository.countByEventSectionId(SECTION_ID)).thenReturn(5L);

        save(layout);

        verify(eventLayoutRepository).save(any());
        verify(seatRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatBlockRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatRepository, never()).insertAll(any());
        verify(eventSectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("si faltan sillas en la base, la sección se regenera aunque el layout no cambie")
    void regeneratesWhenSeatsAreMissing() {
        when(seatBlockRepository.save(any())).thenReturn(SeatBlock.builder().seatBlockId(33).build());
        String layout = layout(new int[]{3, 2});
        previousLayout(layout);
        when(seatRepository.countByEventSectionId(SECTION_ID)).thenReturn(0L);

        save(layout);

        assertThat(insertedSeats()).hasSize(3);
    }

    @Test
    @DisplayName("cambiar la cantidad, la numeración o las cortesías regenera la sección")
    void regeneratesChangedSections() {
        when(seatBlockRepository.save(any())).thenReturn(SeatBlock.builder().seatBlockId(33).build());
        previousLayout(layoutWithCourtesy(SECTION_ID, 0, new int[]{3, 2}));

        save(layoutWithCourtesy(SECTION_ID, 1, new int[]{3, 2}));

        verify(seatRepository, never()).countByEventSectionId(anyInt());
        assertThat(insertedSeats()).extracting(Seat::getStatus).containsExactly(
                SeatStatus.AVAILABLE, SeatStatus.AVAILABLE, SeatStatus.COURTESY);
    }

    @Test
    @DisplayName("un layout anterior ilegible o sin elementos no impide guardar: se regenera todo")
    void regeneratesWhenPreviousLayoutIsUnreadable() {
        when(seatBlockRepository.save(any())).thenReturn(SeatBlock.builder().seatBlockId(33).build());
        previousLayout("{no es json");
        save(layout(new int[]{2, 2}));
        assertThat(insertedSeats()).hasSize(2);
    }

    @Test
    @DisplayName("un layout anterior sin lista de elementos se trata como vacío")
    void regeneratesWhenPreviousLayoutHasNoElements() {
        when(seatBlockRepository.save(any())).thenReturn(SeatBlock.builder().seatBlockId(33).build());
        previousLayout("{\"canvasWidth\":1200}");
        save(layout(new int[]{2, 2}));
        assertThat(insertedSeats()).hasSize(2);
    }

    @Test
    @DisplayName("rechaza secciones de otro evento sin tocar sus sillas")
    void rejectsSectionFromAnotherEvent() {
        assertThatThrownBy(() -> save(layoutForSection(999, new int[]{10, 5})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no pertenece a este evento");

        verify(seatRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatBlockRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatRepository, never()).insertAll(any());
    }

    @Test
    @DisplayName("rechaza un sub-elemento con más sillas que el máximo")
    void rejectsElementOverLimit() {
        assertThatThrownBy(() -> save(layout(new int[]{SaveEventLayoutService.MAX_SEATS_PER_SECTION + 1, 100})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("más de");

        verify(seatRepository, never()).insertAll(any());
    }

    @Test
    @DisplayName("rechaza una sección que supera el máximo sumando sus sub-elementos")
    void rejectsSectionOverLimitAcrossSubElements() {
        int half = SaveEventLayoutService.MAX_SEATS_PER_SECTION / 2 + 1;

        assertThatThrownBy(() -> save(layout(new int[]{half, 100}, new int[]{half, 100})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("más de");

        verify(seatRepository, never()).deleteAllByEventSectionId(anyInt());
        verify(seatRepository, never()).insertAll(any());
    }
}
