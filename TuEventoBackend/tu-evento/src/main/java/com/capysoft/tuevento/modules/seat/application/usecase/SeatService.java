package com.capysoft.tuevento.modules.seat.application.usecase;

import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatRequest;
import com.capysoft.tuevento.modules.seat.application.dto.request.UpdateSeatStatusRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatLogResponse;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatResponse;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatLog;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatLogRepository;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService implements SeatUseCase {

    private final SeatRepository    seatRepository;
    private final SeatLogRepository seatLogRepository;

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
    public SeatResponse createSeat(CreateSeatRequest request) {
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
        Seat existing = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("SEAT_NOT_FOUND", "La silla no existe"));

        if (existing.getStatus() == request.getNewStatus()) {
            throw new BusinessException("SEAT_STATUS_UNCHANGED",
                    "La silla ya tiene ese estado");
        }

        seatLogRepository.save(SeatLog.builder()
                .seatId(seatId)
                .oldStatus(existing.getStatus())
                .newStatus(request.getNewStatus())
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .reason(request.getReason())
                .build());

        Seat updated = Seat.builder()
                .seatId(existing.getSeatId())
                .seatBlockId(existing.getSeatBlockId())
                .eventSectionId(existing.getEventSectionId())
                .code(existing.getCode())
                .row(existing.getRow())
                .position(existing.getPosition())
                .type(existing.getType())
                .status(request.getNewStatus())
                .reservedBy(existing.getReservedBy())
                .reservedUntil(existing.getReservedUntil())
                .build();

        return toResponse(seatRepository.save(updated));
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
    public void deleteSeat(Integer seatId) {
        if (seatRepository.findById(seatId).isEmpty()) {
            throw new NotFoundException("SEAT_NOT_FOUND", "La silla no existe");
        }
        seatRepository.deleteById(seatId);
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
