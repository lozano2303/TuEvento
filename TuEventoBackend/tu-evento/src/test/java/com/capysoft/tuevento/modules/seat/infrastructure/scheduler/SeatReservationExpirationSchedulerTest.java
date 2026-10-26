package com.capysoft.tuevento.modules.seat.infrastructure.scheduler;

import com.capysoft.tuevento.modules.seat.domain.event.SeatStatusChangedEvent;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Liberación automática de reservas vencidas: cada silla se relee bloqueada y solo se libera
 * si sigue RESERVED y vencida, para no pisar una venta confirmada entre la consulta y el guardado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeatReservationExpirationScheduler — libera solo lo que sigue vencido")
class SeatReservationExpirationSchedulerTest {

    private static final int SECTION_ID = 70;
    private static final int EVENT_ID = 5;

    @Mock SeatRepository seatRepository;
    @Mock EventSectionRepository eventSectionRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks SeatReservationExpirationScheduler scheduler;

    private static Seat seat(int seatId, SeatStatus status, LocalDateTime reservedUntil) {
        return Seat.builder().seatId(seatId).eventSectionId(SECTION_ID).code("S" + seatId)
                .status(status).reservedBy(status == SeatStatus.RESERVED ? 7 : null)
                .reservedUntil(reservedUntil).build();
    }

    @Test
    @DisplayName("libera las vencidas en orden ascendente y salta las que cambiaron mientras tanto")
    void releasesOnlyStillExpiredSeats() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        when(seatRepository.findAllByStatusAndReservedUntilBefore(eq(SeatStatus.RESERVED), any()))
                .thenReturn(List.of(seat(3, SeatStatus.RESERVED, past), seat(1, SeatStatus.RESERVED, past),
                        seat(2, SeatStatus.RESERVED, past), seat(4, SeatStatus.RESERVED, past)));
        // 1: sigue vencida; 2: se vendió; 3: se renovó; 4: se borró
        when(seatRepository.findByIdForUpdate(1)).thenReturn(Optional.of(seat(1, SeatStatus.RESERVED, past)));
        when(seatRepository.findByIdForUpdate(2)).thenReturn(Optional.of(seat(2, SeatStatus.SOLD, null)));
        when(seatRepository.findByIdForUpdate(3)).thenReturn(Optional.of(
                seat(3, SeatStatus.RESERVED, LocalDateTime.now().plusMinutes(5))));
        when(seatRepository.findByIdForUpdate(4)).thenReturn(Optional.empty());
        when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(
                EventSection.builder().eventSectionId(SECTION_ID).eventId(EVENT_ID).build()));

        scheduler.releaseExpiredReservations();

        InOrder order = inOrder(seatRepository);
        order.verify(seatRepository).findByIdForUpdate(1);
        order.verify(seatRepository).findByIdForUpdate(2);
        order.verify(seatRepository).findByIdForUpdate(3);
        order.verify(seatRepository).findByIdForUpdate(4);

        ArgumentCaptor<Seat> saved = ArgumentCaptor.forClass(Seat.class);
        verify(seatRepository).save(saved.capture());
        assertThat(saved.getValue().getSeatId()).isEqualTo(1);
        assertThat(saved.getValue().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(saved.getValue().getReservedBy()).isNull();

        ArgumentCaptor<SeatStatusChangedEvent> event = ArgumentCaptor.forClass(SeatStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().getSeatId()).isEqualTo(1);
        assertThat(event.getValue().getEventId()).isEqualTo(EVENT_ID);
    }

    @Test
    @DisplayName("sin sección (dato huérfano) libera la silla sin publicar")
    void orphanSeatIsReleasedWithoutPublishing() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        when(seatRepository.findAllByStatusAndReservedUntilBefore(eq(SeatStatus.RESERVED), any()))
                .thenReturn(List.of(seat(1, SeatStatus.RESERVED, past)));
        when(seatRepository.findByIdForUpdate(1)).thenReturn(Optional.of(seat(1, SeatStatus.RESERVED, past)));
        when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        scheduler.releaseExpiredReservations();

        verify(seatRepository).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("sin reservas vencidas no bloquea ni guarda nada")
    void nothingExpired() {
        when(seatRepository.findAllByStatusAndReservedUntilBefore(eq(SeatStatus.RESERVED), any()))
                .thenReturn(List.of());

        scheduler.releaseExpiredReservations();

        verify(seatRepository, never()).findByIdForUpdate(any());
        verify(seatRepository, never()).save(any());
    }
}
