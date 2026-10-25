package com.capysoft.tuevento.modules.section.application.port.in;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.EventSectionResponse;

import java.util.List;

public interface EventSectionUseCase {
    List<EventSectionResponse> getSectionsByEvent(Integer eventId, String languageCode);
    EventSectionResponse createEventSection(CreateEventSectionRequest request, Long userId);
    EventSectionResponse updateEventSection(Integer eventSectionId, UpdateEventSectionRequest request, Long userId);
    void deleteEventSection(Integer eventSectionId, Long userId);
}
