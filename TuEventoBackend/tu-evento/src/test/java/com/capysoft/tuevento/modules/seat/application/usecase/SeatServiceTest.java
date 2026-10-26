package com.capysoft.tuevento.modules.seat.application.usecase;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatRequest;
import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;
import com.capysoft.tuevento.modules.seat.domain.event.SeatStatusChangedEvent;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatLogRepository;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cambios de estado de las sillas: los flujos del sistema validan la transición y la
 * publican para el mapa en vivo; a mano solo el organizador del evento en DRAFT y solo
 * para marcar o desmarcar cortesías.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeatService — estados, propiedad y WebSocket")
class SeatServiceTest {

    private static final int SEAT_ID = 10;
    private static final int SECTION_ID = 70;
    private static final int EVENT_ID = 5;
    private static final long OWNER_ID = 9L;
    private static final long OTHER_ID = 99L;

    @Mock SeatRepository seatRepository;
    @Mock SeatLogRepository seatLogRepository;
    @Mock EventSectionRepository eventSectionRepository;
    @Mock EventRepository eventRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks SeatService service;

    @BeforeEach
    void setUp() {
        lenient().when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(
                EventSection.builder().eventSectionId(SECTION_ID).eventId(EVENT_ID).build()));
        lenient().when(seatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void seatIs(SeatStatus status) {
        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).seatBlockId(1).eventSectionId(SECTION_ID).code("A1").row(1).position(1)
                .type(SeatType.REGULAR).status(status)
                .reservedBy(status == SeatStatus.RESERVED ? 7 : null)
                .reservedUntil(status == SeatStatus.RESERVED ? LocalDateTime.now().plusMinutes(5) : null)
                .build()));
    }

    private void eventIs(EventStatus status) {
        when(eventRepository.findById((long) EVENT_ID)).thenReturn(Optional.of(
                Event.builder().eventId((long) EVENT_ID).userId(OWNER_ID).status(status).build()));
    }

    private static UpdateSeatStatusRequest to(SeatStatus status) {
        return UpdateSeatStatusRequest.builder().newStatus(status).reason("prueba").build();
    }

    @Nested
    @DisplayName("flujos del sistema")
    class SystemFlows {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"RESERVED,AVAILABLE", "SOLD,AVAILABLE", "COURTESY,AVAILABLE", "AVAILABLE,COURTESY"})
        @DisplayName("aplican la transición y la publican para el mapa en vivo")
        void appliesAndPublishes(SeatStatus from, SeatStatus to) {
            seatIs(from);

            SeatResponse response = service.updateSeatStatus(SEAT_ID, to(to), null);

            assertThat(response.getStatus()).isEqualTo(to);
            ArgumentCaptor<SeatStatusChangedEvent> event = ArgumentCaptor.forClass(SeatStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(event.capture());
            assertThat(event.getValue().getEventId()).isEqualTo(EVENT_ID);
            assertThat(event.getValue().getOldStatus()).isEqualTo(from);
            assertThat(event.getValue().getNewStatus()).isEqualTo(to);
            verify(seatLogRepository).save(any());
        }

        @Test
        @DisplayName("al salir de RESERVED se limpian los datos de la reserva")
        void clearsReservation() {
            seatIs(SeatStatus.RESERVED);

            SeatResponse response = service.updateSeatStatus(SEAT_ID, to(SeatStatus.AVAILABLE), null);

            assertThat(response.getReservedBy()).isNull();
            assertThat(response.getReservedUntil()).isNull();
        }

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"SOLD,RESERVED", "SOLD,COURTESY", "COURTESY,SOLD", "COURTESY,RESERVED", "RESERVED,COURTESY",
                "RESERVED,SOLD", "AVAILABLE,SOLD", "SOLD,SOLD"})
        @DisplayName("rechazan transiciones sin sentido")
        void rejectsInvalidTransitions(SeatStatus from, SeatStatus to) {
            seatIs(from);

            assertThatThrownBy(() -> service.updateSeatStatus(SEAT_ID, to(to), null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no puede pasar a");
            verify(seatRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("liberar una silla ya libre no falla ni publica nada")
        void sameStatusIsIdempotent() {
            seatIs(SeatStatus.AVAILABLE);

            SeatResponse response = service.updateSeatStatus(SEAT_ID, to(SeatStatus.AVAILABLE), null);

            assertThat(response.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            verify(seatRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("una silla inexistente da 404")
        void missingSeat() {
            when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateSeatStatus(SEAT_ID, to(SeatStatus.SOLD), null))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("sin sección (dato huérfano) guarda el cambio sin publicarlo")
        void orphanSeatIsSavedWithoutPublishing() {
            seatIs(SeatStatus.RESERVED);
            when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

            service.updateSeatStatus(SEAT_ID, to(SeatStatus.AVAILABLE), null);

            verify(seatRepository).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("venta al confirmarse el pago")
    class Sale {

        private static final long BUYER_ID = 7L;

        @Test
        @DisplayName("vende la silla reservada por el comprador y lo publica")
        void sellsSeatReservedByTheBuyer() {
            seatIs(SeatStatus.RESERVED); // reservada por el usuario 7

            SeatResponse response = service.sellSeat(SEAT_ID, BUYER_ID, "pago");

            assertThat(response.getStatus()).isEqualTo(SeatStatus.SOLD);
            assertThat(response.getReservedBy()).isNull();
            ArgumentCaptor<SeatStatusChangedEvent> event = ArgumentCaptor.forClass(SeatStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(event.capture());
            assertThat(event.getValue().getNewStatus()).isEqualTo(SeatStatus.SOLD);
            assertThat(event.getValue().getChangedBy()).isEqualTo(7);
        }

        @Test
        @DisplayName("vende una silla libre si la reserva expiró y nadie la tomó")
        void sellsFreeSeatAfterExpiredReservation() {
            seatIs(SeatStatus.AVAILABLE);
            assertThat(service.sellSeat(SEAT_ID, BUYER_ID, "pago").getStatus()).isEqualTo(SeatStatus.SOLD);
        }

        @Test
        @DisplayName("no vende una silla reservada por otra persona (doble venta)")
        void rejectsSeatReservedBySomeoneElse() {
            seatIs(SeatStatus.RESERVED);

            assertThatThrownBy(() -> service.sellSeat(SEAT_ID, 8L, "pago"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya no está disponible");
            verify(seatRepository, never()).save(any());
        }

        @ParameterizedTest(name = "silla {0}")
        @CsvSource({"SOLD", "COURTESY"})
        @DisplayName("no vende una silla ya vendida o de cortesía, ni lo da por hecho")
        void rejectsSoldOrCourtesySeats(SeatStatus status) {
            seatIs(status);

            assertThatThrownBy(() -> service.sellSeat(SEAT_ID, BUYER_ID, "pago"))
                    .isInstanceOf(BusinessException.class);
            verify(seatRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("cambios manuales del organizador")
    class ManualChanges {

        @Test
        @DisplayName("el dueño marca y desmarca una cortesía con el evento en DRAFT")
        void ownerTogglesCourtesy() {
            seatIs(SeatStatus.AVAILABLE);
            eventIs(EventStatus.DRAFT);

            SeatResponse response = service.updateSeatStatusByOrganizer(SEAT_ID, to(SeatStatus.COURTESY), OWNER_ID);

            assertThat(response.getStatus()).isEqualTo(SeatStatus.COURTESY);
            verify(eventPublisher).publishEvent(any(SeatStatusChangedEvent.class));
        }

        @Test
        @DisplayName("otro organizador no puede tocar sillas de un evento ajeno")
        void otherOrganizerIsForbidden() {
            seatIs(SeatStatus.AVAILABLE);
            eventIs(EventStatus.DRAFT);

            assertThatThrownBy(() -> service.updateSeatStatusByOrganizer(SEAT_ID, to(SeatStatus.COURTESY), OTHER_ID))
                    .isInstanceOf(ForbiddenException.class);
            verify(seatRepository, never()).save(any());
        }

        @Test
        @DisplayName("con el evento publicado no se pueden cambiar a mano")
        void publishedEventIsRejected() {
            seatIs(SeatStatus.AVAILABLE);
            eventIs(EventStatus.PUBLISHED);

            assertThatThrownBy(() -> service.updateSeatStatusByOrganizer(SEAT_ID, to(SeatStatus.COURTESY), OWNER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("una venta no se puede deshacer a mano (antes permitía revender una silla vendida)")
        void soldSeatCannotBeReleasedByHand() {
            seatIs(SeatStatus.SOLD);
            eventIs(EventStatus.DRAFT);

            assertThatThrownBy(() -> service.updateSeatStatusByOrganizer(SEAT_ID, to(SeatStatus.AVAILABLE), OWNER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cortesía");
            verify(seatRepository, never()).save(any());
        }

        @Test
        @DisplayName("tampoco se puede vender o reservar a mano")
        void cannotSellByHand() {
            seatIs(SeatStatus.AVAILABLE);
            eventIs(EventStatus.DRAFT);

            assertThatThrownBy(() -> service.updateSeatStatusByOrganizer(SEAT_ID, to(SeatStatus.SOLD), OWNER_ID))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("pedir el mismo estado responde que no cambió")
        void unchanged() {
            seatIs(SeatStatus.COURTESY);
            eventIs(EventStatus.DRAFT);

            assertThatThrownBy(() -> service.updateSeatStatusByOrganizer(SEAT_ID, to(SeatStatus.COURTESY), OWNER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya tiene ese estado");
        }

        @Test
        @DisplayName("una sección o un evento inexistente da 404")
        void missingSectionOrEvent() {
            seatIs(SeatStatus.AVAILABLE);
            when(eventRepository.findById((long) EVENT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateSeatStatusByOrganizer(SEAT_ID, to(SeatStatus.COURTESY), OWNER_ID))
                    .isInstanceOf(NotFoundException.class);

            when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateSeatStatusByOrganizer(SEAT_ID, to(SeatStatus.COURTESY), OWNER_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("crear y borrar sillas")
    class CreateAndDelete {

        private final CreateSeatRequest request = CreateSeatRequest.builder()
                .seatBlockId(1).eventSectionId(SECTION_ID).code("Z9").row(9).position(9).type(SeatType.REGULAR).build();

        @Test
        @DisplayName("el dueño crea una silla disponible")
        void ownerCreates() {
            eventIs(EventStatus.DRAFT);

            SeatResponse response = service.createSeat(request, OWNER_ID);

            assertThat(response.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(response.getCode()).isEqualTo("Z9");
        }

        @Test
        @DisplayName("otro organizador no puede crear sillas en un evento ajeno")
        void otherOrganizerCannotCreate() {
            eventIs(EventStatus.DRAFT);
            assertThatThrownBy(() -> service.createSeat(request, OTHER_ID)).isInstanceOf(ForbiddenException.class);
            verify(seatRepository, never()).save(any());
        }

        @Test
        @DisplayName("no se repite un código en la misma sección")
        void duplicatedCode() {
            eventIs(EventStatus.DRAFT);
            when(seatRepository.existsByEventSectionIdAndCode(SECTION_ID, "Z9")).thenReturn(true);
            assertThatThrownBy(() -> service.createSeat(request, OWNER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ya existe");
        }

        @Test
        @DisplayName("el dueño borra una silla de cortesía")
        void ownerDeletes() {
            seatIs(SeatStatus.COURTESY);
            eventIs(EventStatus.DRAFT);

            service.deleteSeat(SEAT_ID, OWNER_ID);

            verify(seatRepository).deleteById(SEAT_ID);
        }

        @Test
        @DisplayName("no se borra una silla vendida, ni sillas de otro organizador")
        void cannotDeleteSoldOrForeignSeats() {
            seatIs(SeatStatus.SOLD);
            eventIs(EventStatus.DRAFT);
            assertThatThrownBy(() -> service.deleteSeat(SEAT_ID, OWNER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("reservada o vendida");
            assertThatThrownBy(() -> service.deleteSeat(SEAT_ID, OTHER_ID)).isInstanceOf(ForbiddenException.class);
            verify(seatRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("consultas")
    class Queries {

        @Test
        @DisplayName("lista por bloque y por sección, y el historial de una silla existente")
        void lists() {
            Seat seat = Seat.builder().seatId(SEAT_ID).eventSectionId(SECTION_ID).status(SeatStatus.AVAILABLE).build();
            when(seatRepository.findAllBySeatBlockId(1)).thenReturn(List.of(seat));
            when(seatRepository.findAllByEventSectionId(SECTION_ID)).thenReturn(List.of(seat));
            when(seatRepository.findById(SEAT_ID)).thenReturn(Optional.of(seat));
            when(seatLogRepository.findAllBySeatId(SEAT_ID)).thenReturn(List.of());

            assertThat(service.getSeatsByBlock(1)).hasSize(1);
            assertThat(service.getSeatsBySection(SECTION_ID)).extracting(SeatResponse::getSeatId).containsExactly(SEAT_ID);
            assertThat(service.getSeatLog(SEAT_ID)).isEmpty();
        }

        @Test
        @DisplayName("el historial de una silla inexistente da 404")
        void logOfMissingSeat() {
            when(seatRepository.findById(SEAT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getSeatLog(SEAT_ID)).isInstanceOf(NotFoundException.class);
        }
    }
}
