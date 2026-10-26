package com.capysoft.tuevento.modules.seat.application.usecase;

import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;
import com.capysoft.tuevento.modules.seat.domain.event.SeatStatusChangedEvent;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reserva temporal de sillas: la silla se lee siempre bloqueada (findByIdForUpdate) para que
 * dos compradores simultáneos no la ganen ambos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeatReservationService — reserva y liberación con bloqueo")
class SeatReservationServiceTest {

    private static final int SEAT_ID = 10;
    private static final int SECTION_ID = 70;
    private static final int EVENT_ID = 5;
    private static final int OWNER_ID = 7;
    private static final int OTHER_ID = 8;

    @Mock SeatRepository seatRepository;
    @Mock EventSectionRepository eventSectionRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks SeatReservationService service;

    @BeforeEach
    void setUp() {
        lenient().when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(
                EventSection.builder().eventSectionId(SECTION_ID).eventId(EVENT_ID).build()));
        lenient().when(seatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void seatIs(SeatStatus status, Integer reservedBy) {
        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).eventSectionId(SECTION_ID).code("A1").status(status)
                .reservedBy(reservedBy)
                .reservedUntil(reservedBy != null ? LocalDateTime.now().plusMinutes(5) : null)
                .build()));
    }

    @Nested
    @DisplayName("reservar")
    class Reserve {

        @Test
        @DisplayName("una silla libre queda RESERVED a nombre del usuario y se publica")
        void reservesAvailableSeat() {
            seatIs(SeatStatus.AVAILABLE, null);

            SeatResponse response = service.reserveSeat(SEAT_ID, OWNER_ID);

            assertThat(response.getStatus()).isEqualTo(SeatStatus.RESERVED);
            assertThat(response.getReservedBy()).isEqualTo(OWNER_ID);
            assertThat(response.getReservedUntil()).isAfter(LocalDateTime.now().plusMinutes(9));
            ArgumentCaptor<SeatStatusChangedEvent> event = ArgumentCaptor.forClass(SeatStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(event.capture());
            assertThat(event.getValue().getEventId()).isEqualTo(EVENT_ID);
            assertThat(event.getValue().getOldStatus()).isEqualTo(SeatStatus.AVAILABLE);
            verify(seatRepository, never()).findById(anyInt());
        }

        @Test
        @DisplayName("una silla ya reservada se rechaza sin guardar nada")
        void rejectsTakenSeat() {
            seatIs(SeatStatus.RESERVED, OTHER_ID);

            assertThatThrownBy(() -> service.reserveSeat(SEAT_ID, OWNER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está disponible");
            verify(seatRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("una silla inexistente da 404")
        void missingSeat() {
            when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.reserveSeat(SEAT_ID, OWNER_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("sin sección (dato huérfano) da 404")
        void orphanSeat() {
            seatIs(SeatStatus.AVAILABLE, null);
            when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.reserveSeat(SEAT_ID, OWNER_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("liberar")
    class Release {

        @Test
        @DisplayName("el dueño libera su reserva y la silla vuelve a AVAILABLE")
        void ownerReleases() {
            seatIs(SeatStatus.RESERVED, OWNER_ID);

            SeatResponse response = service.releaseSeat(SEAT_ID, OWNER_ID, false);

            assertThat(response.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
            assertThat(response.getReservedBy()).isNull();
            assertThat(response.getReservedUntil()).isNull();
            verify(eventPublisher).publishEvent(any(SeatStatusChangedEvent.class));
        }

        @Test
        @DisplayName("un admin libera la reserva de otro usuario")
        void adminReleasesOthers() {
            seatIs(SeatStatus.RESERVED, OTHER_ID);
            assertThat(service.releaseSeat(SEAT_ID, OWNER_ID, true).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        }

        @Test
        @DisplayName("nadie más puede liberar la reserva ajena")
        void othersCannotRelease() {
            seatIs(SeatStatus.RESERVED, OTHER_ID);
            assertThatThrownBy(() -> service.releaseSeat(SEAT_ID, OWNER_ID, false))
                    .isInstanceOf(BusinessException.class);
            verify(seatRepository, never()).save(any());
        }

        @Test
        @DisplayName("una silla que no está reservada no se libera")
        void notReserved() {
            seatIs(SeatStatus.SOLD, null);
            assertThatThrownBy(() -> service.releaseSeat(SEAT_ID, OWNER_ID, true))
                    .isInstanceOf(BusinessException.class);
            verify(seatRepository, never()).save(any());
        }
    }
}
