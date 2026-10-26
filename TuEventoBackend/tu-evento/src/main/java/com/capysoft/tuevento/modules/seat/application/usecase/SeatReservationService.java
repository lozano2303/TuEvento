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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio para gestionar reservas temporales de sillas con TTL (Time-To-Live).
 * 
 * Las reservas tienen una duración de 10 minutos, tras lo cual se liberan automáticamente
 * vía el scheduler SeatReservationExpirationScheduler.
 */
@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final SeatRepository seatRepository;
    private final EventSectionRepository eventSectionRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final int RESERVATION_TTL_MINUTES = 10;

    /**
     * Reserva una silla para el usuario autenticado.
     * 
     * @param seatId ID de la silla a reservar
     * @param userId ID del usuario que realiza la reserva
     * @return La silla reservada
     * @throws NotFoundException si la silla no existe
     * @throws BusinessException si la silla no está disponible
     */
    @Transactional
    public SeatResponse reserveSeat(Integer seatId, Integer userId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("SEAT_NOT_FOUND", "La silla no existe"));

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessException("SEAT_NOT_AVAILABLE", 
                    "La silla no está disponible para reserva");
        }

        SeatStatus oldStatus = seat.getStatus();
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(RESERVATION_TTL_MINUTES);

        Seat reserved = Seat.builder()
                .seatId(seat.getSeatId())
                .seatBlockId(seat.getSeatBlockId())
                .eventSectionId(seat.getEventSectionId())
                .code(seat.getCode())
                .row(seat.getRow())
                .position(seat.getPosition())
                .type(seat.getType())
                .status(SeatStatus.RESERVED)
                .reservedBy(userId)
                .reservedUntil(reservedUntil)
                .build();

        Seat saved = seatRepository.save(reserved);

        // Obtener eventId para el evento de WebSocket
        EventSection section = eventSectionRepository.findById(seat.getEventSectionId())
                .orElseThrow(() -> new NotFoundException("EVENT_SECTION_NOT_FOUND", 
                        "La sección del evento no existe"));

        publishSeatStatusChangedEvent(saved, oldStatus, userId, section.getEventId());

        return toResponse(saved);
    }

    /**
     * Libera una reserva de silla antes de su expiración.
     * 
     * Solo el usuario que realizó la reserva puede liberarla (excepto ADMIN).
     * 
     * @param seatId ID de la silla a liberar
     * @param userId ID del usuario que solicita la liberación
     * @return La silla liberada
     * @throws NotFoundException si la silla no existe
     * @throws BusinessException si la silla no está reservada o si el usuario no es el dueño
     */
    @Transactional
    public SeatResponse releaseSeat(Integer seatId, Integer userId, boolean isAdmin) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("SEAT_NOT_FOUND", "La silla no existe"));

        if (seat.getStatus() != SeatStatus.RESERVED) {
            throw new BusinessException("SEAT_NOT_RESERVED", 
                    "La silla no está reservada");
        }

        // Verificar que el usuario es el dueño de la reserva (excepto ADMIN)
        if (!isAdmin && !seat.getReservedBy().equals(userId)) {
            throw new BusinessException("UNAUTHORIZED_RELEASE", 
                    "No puedes liberar la reserva de otro usuario");
        }

        SeatStatus oldStatus = seat.getStatus();

        Seat released = Seat.builder()
                .seatId(seat.getSeatId())
                .seatBlockId(seat.getSeatBlockId())
                .eventSectionId(seat.getEventSectionId())
                .code(seat.getCode())
                .row(seat.getRow())
                .position(seat.getPosition())
                .type(seat.getType())
                .status(SeatStatus.AVAILABLE)
                .reservedBy(null)
                .reservedUntil(null)
                .build();

        Seat saved = seatRepository.save(released);

        // Obtener eventId para el evento de WebSocket
        EventSection section = eventSectionRepository.findById(seat.getEventSectionId())
                .orElseThrow(() -> new NotFoundException("EVENT_SECTION_NOT_FOUND", 
                        "La sección del evento no existe"));

        publishSeatStatusChangedEvent(saved, oldStatus, userId, section.getEventId());

        return toResponse(saved);
    }

    private void publishSeatStatusChangedEvent(Seat seat, SeatStatus oldStatus, 
                                               Integer changedBy, Integer eventId) {
        eventPublisher.publishEvent(SeatStatusChangedEvent.builder()
                .seatId(seat.getSeatId())
                .eventSectionId(seat.getEventSectionId())
                .eventId(eventId)
                .oldStatus(oldStatus)
                .newStatus(seat.getStatus())
                .changedBy(changedBy)
                .reservedUntil(seat.getReservedUntil())
                .build());
    }

    private SeatResponse toResponse(Seat seat) {
        return SeatResponse.builder()
                .seatId(seat.getSeatId())
                .seatBlockId(seat.getSeatBlockId())
                .eventSectionId(seat.getEventSectionId())
                .code(seat.getCode())
                .row(seat.getRow())
                .position(seat.getPosition())
                .type(seat.getType())
                .status(seat.getStatus())
                .reservedBy(seat.getReservedBy())
                .reservedUntil(seat.getReservedUntil())
                .build();
    }
}
