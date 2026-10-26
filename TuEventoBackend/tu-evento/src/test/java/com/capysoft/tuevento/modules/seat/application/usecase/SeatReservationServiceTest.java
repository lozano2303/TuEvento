package com.capysoft.tuevento.modules.seat.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.capysoft.tuevento.modules.seat.domain.event.SeatStatusChangedEvent;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatReservationService — reserva con lock")
class SeatReservationServiceTest {

    private static final int SEAT_ID = 1;

    @Mock SeatRepository seatRepository;
    @Mock EventSectionRepository eventSectionRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks SeatReservationService service;

    @BeforeEach
    void setUp() {
        lenient().when(seatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(eventSectionRepository.findById(anyInt()))
                .thenReturn(Optional.of(EventSection.builder().eventSectionId(5).eventId(9).build()));
    }

    private void seatIs(SeatStatus status, Integer reservedBy) {
        when(seatRepository.findByIdForUpdate(SEAT_ID)).thenReturn(Optional.of(Seat.builder()
                .seatId(SEAT_ID).seatBlockId(1).eventSectionId(5).code("A1").row(1).position(1)
                .type(SeatType.REGULAR).status(status).reservedBy(reservedBy)
                .reservedUntil(reservedBy != null ? LocalDateTime.now().plusMinutes(5) : null).build()));
    }

    @Test
    @DisplayName("reserva una silla libre leyéndola con lock y avisa del cambio")
    void reservesWithLock() {
        seatIs(SeatStatus.AVAILABLE, null);

        assertThat(service.reserveSeat(SEAT_ID, 7).getStatus()).isEqualTo(SeatStatus.RESERVED);

        verify(seatRepository, never()).findById(anyInt());
        verify(eventPublisher).publishEvent(any(SeatStatusChangedEvent.class));
    }

    @Test
    @DisplayName("una silla que no está libre da SEAT_NOT_AVAILABLE y no se guarda")
    void rejectsTakenSeat() {
        seatIs(SeatStatus.RESERVED, 8);

        assertThatThrownBy(() -> service.reserveSeat(SEAT_ID, 7))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "SEAT_NOT_AVAILABLE");
        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("el titular libera su reserva; otro usuario no puede")
    void releaseOnlyByHolder() {
        seatIs(SeatStatus.RESERVED, 7);
        assertThat(service.releaseSeat(SEAT_ID, 7, false).getStatus()).isEqualTo(SeatStatus.AVAILABLE);

        seatIs(SeatStatus.RESERVED, 7);
        assertThatThrownBy(() -> service.releaseSeat(SEAT_ID, 8, false))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "UNAUTHORIZED_RELEASE");
    }
}
