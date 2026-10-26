package com.capysoft.tuevento.modules.section.application.usecase;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.section.application.dto.request.CreateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.EventSectionResponse;
import com.capysoft.tuevento.modules.section.application.port.in.EventSectionUseCase;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.modules.language.domain.repository.SectionTypeTranslationRepository;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSectionService implements EventSectionUseCase {

    private final EventSectionRepository eventSectionRepository;
    private final SectionTypeRepository  sectionTypeRepository;
    private final SectionTypeTranslationRepository sectionTypeTranslationRepository;
    private final LanguageRepository languageRepository;
    private final EventRepository eventRepository;

    @Override
    public List<EventSectionResponse> getSectionsByEvent(Integer eventId, String languageCode) {
        return eventSectionRepository.findAllByEventId(eventId).stream()
                .map(section -> toResponse(section, languageCode))
                .toList();
    }

    @Override
    public EventSectionResponse createEventSection(CreateEventSectionRequest request, Long userId) {
        requireEditableOwnedEvent(request.getEventId(), userId);
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
        return toResponse(saved, null); // Admin operation, no translation needed
    }

    @Override
    public EventSectionResponse updateEventSection(Integer eventSectionId,
                                                   UpdateEventSectionRequest request,
                                                   Long userId) {
        EventSection existing = eventSectionRepository.findById(eventSectionId)
                .orElseThrow(() -> new NotFoundException("EVENT_SECTION_NOT_FOUND",
                        "La sección no existe"));
        requireEditableOwnedEvent(existing.getEventId(), userId);

        // Al cambiar la capacidad, las disponibles cambian lo mismo (nunca por debajo de 0);
        // antes quedaban con el valor de la capacidad original
        int capacity = request.getCapacity() != null ? request.getCapacity() : existing.getCapacity();
        int availableSeats = Math.max(0, existing.getAvailableSeats() + capacity - existing.getCapacity());

        EventSection updated = EventSection.builder()
                .eventSectionId(existing.getEventSectionId())
                .eventId(existing.getEventId())
                .sectionTypeId(existing.getSectionTypeId())
                .capacity(capacity)
                .availableSeats(availableSeats)
                .price(request.getPrice() != null
                        ? request.getPrice() : existing.getPrice())
                .isActive(request.getIsActive() != null
                        ? request.getIsActive() : existing.getIsActive())
                .build();

        return toResponse(eventSectionRepository.save(updated), null); // Admin operation, no translation needed
    }

    @Override
    public void deleteEventSection(Integer eventSectionId, Long userId) {
        EventSection existing = eventSectionRepository.findById(eventSectionId)
                .orElseThrow(() -> new NotFoundException("EVENT_SECTION_NOT_FOUND",
                        "La sección no existe"));
        requireEditableOwnedEvent(existing.getEventId(), userId);
        eventSectionRepository.deleteById(eventSectionId);
    }

    /**
     * Solo el organizador dueño del evento modifica sus secciones, y solo en DRAFT
     * (igual que el layout): una vez publicado, cambiar precios o aforo afectaría a
     * compras en curso.
     */
    private void requireEditableOwnedEvent(Integer eventId, Long userId) {
        Event event = eventRepository.findById(eventId.longValue())
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND", "El evento no existe"));
        if (!event.getUserId().equals(userId)) {
            throw new ForbiddenException("EVENT_ACCESS_DENIED",
                    "No eres el organizador de este evento");
        }
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("EVENT_SECTION_EDIT_NOT_ALLOWED",
                    "Las secciones solo se pueden modificar cuando el evento está en DRAFT");
        }
    }

    private EventSectionResponse toResponse(EventSection domain, String languageCode) {
        String typeName = sectionTypeRepository.findById(domain.getSectionTypeId())
                .map(sectionType -> sectionType.getName())
                .orElse("Desconocido");
        
        // Si se solicita traducción y no es español, intentar cargar traducción
        if (languageCode != null && !languageCode.isEmpty() && !"es".equalsIgnoreCase(languageCode)) {
            var languageOpt = languageRepository.findByCode(languageCode.toLowerCase());
            if (languageOpt.isPresent()) {
                Long sectionTypeIdLong = Long.valueOf(domain.getSectionTypeId());
                Integer languageId = languageOpt.get().getLanguageId();
                
                var translationOpt = sectionTypeTranslationRepository
                        .findBySectionTypeAndLanguage(sectionTypeIdLong, languageId);
                if (translationOpt.isPresent()) {
                    typeName = translationOpt.get().getTranslatedName();
                }
            }
        }
        
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
