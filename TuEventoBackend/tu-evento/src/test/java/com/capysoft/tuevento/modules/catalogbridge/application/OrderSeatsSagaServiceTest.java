package com.capysoft.tuevento.modules.catalogbridge.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.catalogbridge.application.OrderSeatsSagaService.SeatsSaleRejected;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxEventPublisher;

@DisplayName("OrderSeatsSagaService — vender y liberar las sillas de una orden")
class OrderSeatsSagaServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    private final SeatUseCase seatUseCase = mock(SeatUseCase.class);
    private final SeatRepository seats = mock(SeatRepository.class);
    private final ProcessedEventStore processed = mock(ProcessedEventStore.class);
    private final OutboxEventPublisher outbox = mock(OutboxEventPublisher.class);
    private final OrderSeatsSagaService saga = new OrderSeatsSagaService(seatUseCase, seats, processed, outbox);

    private void seat(int id, SeatStatus status, Integer reservedBy) {
        when(seats.findByIdForUpdate(id)).thenReturn(Optional.of(Seat.builder().seatId(id).status(status)
                .reservedBy(reservedBy).build()));
    }

    @Test
    @DisplayName("vende todas las sillas y marca el mensaje")
    void sells() {
        saga.sell(EVENT_ID, 1L, 5L, List.of(10, 11), "paid");

        verify(seatUseCase).sellSeat(10, 5L, "paid");
        verify(seatUseCase).sellSeat(11, 5L, "paid");
        verify(processed).markProcessed(OrderSeatsSagaService.CONSUMER, EVENT_ID);
    }

    @Test
    @DisplayName("sin eventId vende igual, sin marcar")
    void sellsWithoutEventId() {
        saga.sell(null, 1L, 5L, List.of(10), "paid");

        verify(seatUseCase).sellSeat(10, 5L, "paid");
        verifyNoInteractions(processed);
    }

    @Test
    @DisplayName("rechazar la venta publica SeatsSaleRejected y marca el mensaje")
    void rejects() {
        saga.rejectSale(EVENT_ID, 1L, List.of(10), new BusinessException("SEAT_ALREADY_TAKEN", "taken"));

        verify(outbox).publish("seat", "1", "SeatsSaleRejected",
                new SeatsSaleRejected(1L, List.of(10), "SEAT_ALREADY_TAKEN"));
        verify(processed).markProcessed(OrderSeatsSagaService.CONSUMER, EVENT_ID);
    }

    @Test
    @DisplayName("libera solo las sillas que siguen reservadas por el comprador")
    void releasesOnlyOwnReservations() {
        seat(10, SeatStatus.RESERVED, 5);
        seat(11, SeatStatus.RESERVED, 6);
        seat(12, SeatStatus.SOLD, 5);
        seat(13, SeatStatus.RESERVED, null);
        when(seats.findByIdForUpdate(14)).thenReturn(Optional.empty());

        saga.release(EVENT_ID, 1L, 5L, List.of(10, 11, 12, 13, 14), null, "Order cancelled");

        verify(seatUseCase).updateSeatStatus(eq(10), any(), eq(null));
        verify(seatUseCase, never()).updateSeatStatus(eq(11), any(), any());
        verify(seatUseCase, never()).updateSeatStatus(eq(12), any(), any());
        verify(seatUseCase, never()).updateSeatStatus(eq(13), any(), any());
        verify(seatUseCase, never()).updateSeatStatus(eq(14), any(), any());
        verify(processed).markProcessed(OrderSeatsSagaService.CONSUMER, EVENT_ID);
    }
}
