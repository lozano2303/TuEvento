package com.capysoft.tuevento.modules.seat.application.usecase;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatRequest;
import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatLogResponse;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.event.SeatStatusChangedEvent;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatLog;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatLogRepository;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatService implements SeatUseCase {

    /**
     * Transiciones que pueden hacer los flujos del sistema (pago, cancelación, pago fallido,
     * reembolso). AVAILABLE → SOLD se permite: si la reserva expira justo antes de que llegue
     * el pago aprobado, la confirmación de quien ya pagó no puede fallar.
     * Una cortesía solo vuelve a estar disponible; una venta nunca vuelve a reservada.
     */
    private static final Map<SeatStatus, Set<SeatStatus>> SYSTEM_TRANSITIONS = Map.of(
            SeatStatus.AVAILABLE, EnumSet.of(SeatStatus.RESERVED, SeatStatus.SOLD, SeatStatus.COURTESY),
            SeatStatus.RESERVED,  EnumSet.of(SeatStatus.AVAILABLE, SeatStatus.SOLD),
            SeatStatus.SOLD,      EnumSet.of(SeatStatus.AVAILABLE),
            SeatStatus.COURTESY,  EnumSet.of(SeatStatus.AVAILABLE));

    /** A mano, el organizador solo marca o desmarca cortesías. Lo demás lo hacen las compras. */
    private static final Set<SeatStatus> MANUAL_STATUSES = EnumSet.of(SeatStatus.AVAILABLE, SeatStatus.COURTESY);

    private final SeatRepository            seatRepository;
    private final SeatLogRepository         seatLogRepository;
    private final EventSectionRepository    eventSectionRepository;
    private final EventRepository           eventRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<SeatResponse> getSeatsByBlock(Integer seatBlockId) {
        return seatRepository.findAllBySeatBlockId(seatBlockId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<SeatResponse> getSeatsBySection(Integer eventSectionId) {
        return seatRepository.findAllByEventSectionId(eventSectionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SeatResponse createSeat(CreateSeatRequest request, Long organizerId) {
        requireEditableOwnedSection(request.getEventSectionId(), organizerId);
        if (seatRepository.existsByEventSectionIdAndCode(
                request.getEventSectionId(), request.getCode())) {
            throw new BusinessException("SEAT_CODE_ALREADY_EXISTS",
                    "Ya existe una silla con ese código en esta sección");
        }
        Seat saved = seatRepository.save(Seat.builder()
                .seatBlockId(request.getSeatBlockId())
                .eventSectionId(request.getEventSectionId())
                .code(request.getCode())
                .row(request.getRow())
                .position(request.getPosition())
                .type(request.getType())
                .status(SeatStatus.AVAILABLE)
                .build());
        return toResponse(saved);
    }

    @Override
    public SeatResponse updateSeatStatus(Integer seatId, UpdateSeatStatusRequest request,
                                         Integer changedBy) {
        Seat existing = findSeat(seatId);
        SeatStatus newStatus = request.getNewStatus();

        // Idempotente: un flujo que repite el cambio (p. ej. la silla ya se liberó al expirar
        // la reserva) no debe romperse
        if (existing.getStatus() == newStatus) {
            return toResponse(existing);
        }
        if (!SYSTEM_TRANSITIONS.get(existing.getStatus()).contains(newStatus)) {
            throw new BusinessException("SEAT_STATUS_TRANSITION_NOT_ALLOWED",
                    "Una silla " + existing.getStatus() + " no puede pasar a " + newStatus);
        }
        return applyStatus(existing, newStatus, request.getReason(), changedBy);
    }

    @Override
    public SeatResponse updateSeatStatusByOrganizer(Integer seatId, UpdateSeatStatusRequest request,
                                                    Long organizerId) {
        Seat existing = findSeat(seatId);
        EventSection section = requireEditableOwnedSection(existing.getEventSectionId(), organizerId);
        SeatStatus newStatus = request.getNewStatus();

        if (existing.getStatus() == newStatus) {
            throw new BusinessException("SEAT_STATUS_UNCHANGED", "La silla ya tiene ese estado");
        }
        if (!MANUAL_STATUSES.contains(existing.getStatus()) || !MANUAL_STATUSES.contains(newStatus)) {
            throw new BusinessException("SEAT_STATUS_TRANSITION_NOT_ALLOWED",
                    "A mano solo se puede marcar o desmarcar una silla como cortesía");
        }
        return applyStatus(existing, newStatus, request.getReason(), organizerId.intValue(), section.getEventId());
    }

    @Override
    public List<SeatLogResponse> getSeatLog(Integer seatId) {
        if (seatRepository.findById(seatId).isEmpty()) {
            throw new NotFoundException("SEAT_NOT_FOUND", "La silla no existe");
        }
        return seatLogRepository.findAllBySeatId(seatId).stream()
                .map(this::toLogResponse)
                .toList();
    }

    @Override
    public void deleteSeat(Integer seatId, Long organizerId) {
        Seat existing = findSeat(seatId);
        requireEditableOwnedSection(existing.getEventSectionId(), organizerId);
        if (!MANUAL_STATUSES.contains(existing.getStatus())) {
            throw new BusinessException("SEAT_NOT_DELETABLE",
                    "No se puede borrar una silla reservada o vendida");
        }
        seatRepository.deleteById(seatId);
    }

    private Seat findSeat(Integer seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("SEAT_NOT_FOUND", "La silla no existe"));
    }

    /** La sección debe ser de un evento del organizador y el evento debe estar en DRAFT. */
    private EventSection requireEditableOwnedSection(Integer eventSectionId, Long organizerId) {
        EventSection section = eventSectionRepository.findById(eventSectionId)
                .orElseThrow(() -> new NotFoundException("EVENT_SECTION_NOT_FOUND", "La sección del evento no existe"));
        Event event = eventRepository.findById(section.getEventId().longValue())
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND", "El evento no existe"));
        if (!event.getUserId().equals(organizerId)) {
            throw new ForbiddenException("EVENT_ACCESS_DENIED", "No eres el organizador de este evento");
        }
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("SEAT_EDIT_NOT_ALLOWED",
                    "Las sillas solo se pueden modificar cuando el evento está en DRAFT");
        }
        return section;
    }

    private SeatResponse applyStatus(Seat existing, SeatStatus newStatus, String reason, Integer changedBy) {
        Integer eventId = eventSectionRepository.findById(existing.getEventSectionId())
                .map(EventSection::getEventId)
                .orElse(null);
        return applyStatus(existing, newStatus, reason, changedBy, eventId);
    }

    /**
     * Guarda el cambio con su log y lo publica para el WebSocket del mapa de asientos
     * (/topic/events/{eventId}/seats): así los demás compradores ven en vivo las ventas
     * y las sillas que se liberan, no solo las reservas.
     */
    private SeatResponse applyStatus(Seat existing, SeatStatus newStatus, String reason, Integer changedBy,
                                     Integer eventId) {
        seatLogRepository.save(SeatLog.builder()
                .seatId(existing.getSeatId())
                .oldStatus(existing.getStatus())
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .reason(reason)
                .build());

        // Solo RESERVED conserva reservedBy/reservedUntil; los demás estados los limpian
        boolean reserved = newStatus == SeatStatus.RESERVED;
        Seat saved = seatRepository.save(Seat.builder()
                .seatId(existing.getSeatId())
                .seatBlockId(existing.getSeatBlockId())
                .eventSectionId(existing.getEventSectionId())
                .code(existing.getCode())
                .row(existing.getRow())
                .position(existing.getPosition())
                .type(existing.getType())
                .status(newStatus)
                .reservedBy(reserved ? existing.getReservedBy() : null)
                .reservedUntil(reserved ? existing.getReservedUntil() : null)
                .build());

        if (eventId != null) {
            eventPublisher.publishEvent(SeatStatusChangedEvent.builder()
                    .seatId(saved.getSeatId())
                    .eventSectionId(saved.getEventSectionId())
                    .eventId(eventId)
                    .oldStatus(existing.getStatus())
                    .newStatus(newStatus)
                    .changedBy(changedBy)
                    .reservedUntil(saved.getReservedUntil())
                    .build());
        }
        return toResponse(saved);
    }

    private SeatResponse toResponse(Seat domain) {
        return SeatResponse.builder()
                .seatId(domain.getSeatId())
                .seatBlockId(domain.getSeatBlockId())
                .eventSectionId(domain.getEventSectionId())
                .code(domain.getCode())
                .row(domain.getRow())
                .position(domain.getPosition())
                .type(domain.getType())
                .status(domain.getStatus())
                .reservedBy(domain.getReservedBy())
                .reservedUntil(domain.getReservedUntil())
                .build();
    }

    private SeatLogResponse toLogResponse(SeatLog domain) {
        return SeatLogResponse.builder()
                .seatLogId(domain.getSeatLogId())
                .seatId(domain.getSeatId())
                .oldStatus(domain.getOldStatus())
                .newStatus(domain.getNewStatus())
                .changedAt(domain.getChangedAt())
                .changedBy(domain.getChangedBy())
                .reason(domain.getReason())
                .build();
    }
}
