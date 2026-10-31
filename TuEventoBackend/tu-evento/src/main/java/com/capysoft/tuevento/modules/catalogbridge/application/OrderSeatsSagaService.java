package com.capysoft.tuevento.modules.catalogbridge.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Pasos 3' y 4 de la saga, en el lado de Catálogo: vende o libera las sillas de una orden. El efecto
 * y la marca de "ya procesado" van en la misma transacción: al reintentar, una silla ya vendida por
 * este mensaje no se confunde con una vendida a otra persona.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSeatsSagaService {

    public static final String CONSUMER = "catalogo.order-seats";
    static final String AGGREGATE = "seat";
    static final String SEATS_SALE_REJECTED = "SeatsSaleRejected";

    private final SeatUseCase seatUseCase;
    private final SeatRepository seatRepository;
    private final ProcessedEventStore processedEventStore;
    private final OutboxEventPublisher outboxEventPublisher;

    public record SeatsSaleRejected(Long orderId, List<Integer> seatIds, String reason) {
    }

    /** Todas o ninguna: si una silla ya no es de este comprador, se deshace la venta de las demás. */
    @Transactional
    public void sell(UUID eventId, Long orderId, Long buyerId, List<Integer> seatIds, String reason) {
        for (Integer seatId : seatIds) {
            seatUseCase.sellSeat(seatId, buyerId, reason);
        }
        markProcessed(eventId);
    }

    /** Comercio reembolsa al recibirlo. Se llama cuando {@link #sell} falló por una regla de negocio. */
    @Transactional
    public void rejectSale(UUID eventId, Long orderId, List<Integer> seatIds, BusinessException cause) {
        log.warn("Sale of order {} rejected: {}", orderId, cause.getMessage());
        outboxEventPublisher.publish(AGGREGATE, String.valueOf(orderId), SEATS_SALE_REJECTED,
                new SeatsSaleRejected(orderId, seatIds, cause.getCode()));
        markProcessed(eventId);
    }

    /**
     * Solo libera las sillas que siguen reservadas por el comprador: tras expirar la reserva la silla
     * pudo pasar a otra persona, y una orden cancelada no puede quitársela.
     */
    @Transactional
    public void release(UUID eventId, Long orderId, Long buyerId, List<Integer> seatIds, Integer changedBy, String reason) {
        for (Integer seatId : seatIds) {
            if (stillReservedBy(seatId, buyerId)) {
                seatUseCase.updateSeatStatus(seatId, UpdateSeatStatusRequest.builder()
                        .newStatus(SeatStatus.AVAILABLE)
                        .reason(reason)
                        .build(), changedBy);
            } else {
                log.info("Order {} released seat {} that is no longer reserved by its buyer: skipped", orderId, seatId);
            }
        }
        markProcessed(eventId);
    }

    private boolean stillReservedBy(Integer seatId, Long buyerId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId).orElse(null);
        return seat != null && seat.getStatus() == SeatStatus.RESERVED && seat.getReservedBy() != null
                && seat.getReservedBy().longValue() == buyerId;
    }

    private void markProcessed(UUID eventId) {
        if (eventId != null) {
            processedEventStore.markProcessed(CONSUMER, eventId);
        }
    }
}
