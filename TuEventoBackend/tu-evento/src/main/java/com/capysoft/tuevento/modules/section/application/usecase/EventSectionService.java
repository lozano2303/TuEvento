package com.capysoft.tuevento.modules.section.application.usecase;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.EventSectionResponse;
import com.capysoft.tuevento.modules.section.application.port.in.EventSectionUseCase;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSectionService implements EventSectionUseCase {

    private final EventSectionRepository eventSectionRepository;
    private final SectionTypeRepository  sectionTypeRepository;

    @Override
    public List<EventSectionResponse> getSectionsByEvent(Integer eventId) {
        return eventSectionRepository.findAllByEventId(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EventSectionResponse createEventSection(CreateEventSectionRequest request) {
        if (sectionTypeRepository.findById(request.getSectionTypeId()).isEmpty()) {
            throw new NotFoundException("SECTION_TYPE_NOT_FOUND",
                    "El tipo de sección no existe");
        }
        if (eventSectionRepository.existsByEventIdAndSectionTypeId(
                request.getEventId(), request.getSectionTypeId())) {
            throw new BusinessException("EVENT_SECTION_ALREADY_EXISTS",
                    "Ya existe una sección de ese tipo para este evento");
        }
        EventSection saved = eventSectionRepository.save(EventSection.builder()
                .eventId(request.getEventId())
                .sectionTypeId(request.getSectionTypeId())
                .capacity(request.getCapacity())
                .availableSeats(request.getCapacity())
                .price(request.getPrice())
                .isActive(true)
                .build());
        return toResponse(saved);
    }

    @Override
    public EventSectionResponse updateEventSection(Integer eventSectionId,
                                                   UpdateEventSectionRequest request) {
        EventSection existing = eventSectionRepository.findById(eventSectionId)
                .orElseThrow(() -> new NotFoundException("EVENT_SECTION_NOT_FOUND",
                        "La sección no existe"));

        EventSection updated = EventSection.builder()
                .eventSectionId(existing.getEventSectionId())
                .eventId(existing.getEventId())
                .sectionTypeId(existing.getSectionTypeId())
                .capacity(request.getCapacity() != null
                        ? request.getCapacity() : existing.getCapacity())
                .availableSeats(existing.getAvailableSeats())
                .price(request.getPrice() != null
                        ? request.getPrice() : existing.getPrice())
                .isActive(request.getIsActive() != null
                        ? request.getIsActive() : existing.getIsActive())
                .build();

        return toResponse(eventSectionRepository.save(updated));
    }

    @Override
    public void deleteEventSection(Integer eventSectionId) {
        if (eventSectionRepository.findById(eventSectionId).isEmpty()) {
            throw new NotFoundException("EVENT_SECTION_NOT_FOUND",
                    "La sección no existe");
        }
        eventSectionRepository.deleteById(eventSectionId);
    }

    private EventSectionResponse toResponse(EventSection domain) {
        String typeName = sectionTypeRepository.findById(domain.getSectionTypeId())
                .map(SectionType -> SectionType.getName())
                .orElse("Desconocido");
        return EventSectionResponse.builder()
                .eventSectionId(domain.getEventSectionId())
                .eventId(domain.getEventId())
                .sectionTypeName(typeName)
                .capacity(domain.getCapacity())
                .availableSeats(domain.getAvailableSeats())
                .price(domain.getPrice())
                .isActive(domain.getIsActive())
                .build();
    }
}
