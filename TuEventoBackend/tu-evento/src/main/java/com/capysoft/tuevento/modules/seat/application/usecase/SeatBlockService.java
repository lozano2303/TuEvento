package com.capysoft.tuevento.modules.seat.application.usecase;

import com.capysoft.tuevento.modules.seat.application.dto.request.CreateSeatBlockRequest;
import com.capysoft.tuevento.modules.seat.application.dto.response.SeatBlockResponse;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatBlockUseCase;
import com.capysoft.tuevento.modules.seat.domain.model.SeatBlock;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatBlockRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatBlockService implements SeatBlockUseCase {

    private final SeatBlockRepository seatBlockRepository;

    @Override
    public List<SeatBlockResponse> getBlocksBySection(Integer eventSectionId) {
        return seatBlockRepository.findAllByEventSectionId(eventSectionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SeatBlockResponse createSeatBlock(CreateSeatBlockRequest request) {
        if (seatBlockRepository.existsByEventSectionIdAndName(
                request.getEventSectionId(), request.getName())) {
            throw new BusinessException("SEAT_BLOCK_ALREADY_EXISTS",
                    "Ya existe un bloque con ese nombre en esta sección");
        }
        SeatBlock saved = seatBlockRepository.save(SeatBlock.builder()
                .eventSectionId(request.getEventSectionId())
                .name(request.getName())
                .capacity(request.getCapacity())
                .build());
        return toResponse(saved);
    }

    @Override
    public void deleteSeatBlock(Integer seatBlockId) {
        if (seatBlockRepository.findById(seatBlockId).isEmpty()) {
            throw new NotFoundException("SEAT_BLOCK_NOT_FOUND", "El bloque no existe");
        }
        seatBlockRepository.deleteById(seatBlockId);
    }

    private SeatBlockResponse toResponse(SeatBlock domain) {
        return SeatBlockResponse.builder()
                .seatBlockId(domain.getSeatBlockId())
                .eventSectionId(domain.getEventSectionId())
                .name(domain.getName())
                .capacity(domain.getCapacity())
                .build();
    }
}
