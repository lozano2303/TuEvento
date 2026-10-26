package com.capysoft.tuevento.modules.seat.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.capysoft.tuevento.modules.seat.application.port.out.PendingPaymentSeatsPort;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatReservationExpirationScheduler")
class SeatReservationExpirationSchedulerTest {

    @Mock SeatRepository seatRepository;
    @Mock EventSectionRepository eventSectionRepository;
    @Mock PendingPaymentSeatsPort pendingPaymentSeatsPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks SeatReservationExpirationScheduler scheduler;

    private static Seat expired(int seatId) {
        return Seat.builder().seatId(seatId).seatBlockId(1).eventSectionId(5).code("A" + seatId).row(1)
                .position(seatId).type(SeatType.REGULAR).status(SeatStatus.RESERVED).reservedBy(7)
                .reservedUntil(LocalDateTime.now().minusMinutes(1)).build();
    }

    @Test
    @DisplayName("libera las reservas expiradas y conserva las que tienen el pago en curso")
    void keepsSeatsWithPendingPayment() {
        when(seatRepository.findAllByStatusAndReservedUntilBefore(any(), any()))
                .thenReturn(List.of(expired(1), expired(2)));
        when(pendingPaymentSeatsPort.findSeatIdsWithPendingPayment(anyCollection())).thenReturn(Set.of(2));
        when(seatRepository.findByIdForUpdate(1)).thenReturn(Optional.of(expired(1)));
        when(eventSectionRepository.findById(5)).thenReturn(Optional.empty());

        scheduler.releaseExpiredReservations();

        ArgumentCaptor<Seat> saved = ArgumentCaptor.forClass(Seat.class);
        verify(seatRepository).save(saved.capture());
        assertThat(saved.getValue().getSeatId()).isEqualTo(1);
        assertThat(saved.getValue().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(saved.getValue().getReservedBy()).isNull();
        verify(seatRepository, never()).findByIdForUpdate(2);
    }

    @Test
    @DisplayName("si al bloquearla la silla ya cambió de estado, no la toca")
    void skipsSeatChangedBeforeLock() {
        when(seatRepository.findAllByStatusAndReservedUntilBefore(any(), any())).thenReturn(List.of(expired(1)));
        when(pendingPaymentSeatsPort.findSeatIdsWithPendingPayment(anyCollection())).thenReturn(Set.of());
        when(seatRepository.findByIdForUpdate(1)).thenReturn(Optional.of(Seat.builder()
                .seatId(1).eventSectionId(5).status(SeatStatus.SOLD).build()));

        scheduler.releaseExpiredReservations();

        verify(seatRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("sin reservas expiradas no consulta los pagos pendientes")
    void nothingExpired_doesNotQueryPayments() {
        when(seatRepository.findAllByStatusAndReservedUntilBefore(any(), any())).thenReturn(List.of());

        scheduler.releaseExpiredReservations();

        verify(pendingPaymentSeatsPort, never()).findSeatIdsWithPendingPayment(anyCollection());
    }
}
