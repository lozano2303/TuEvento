package com.capysoft.tuevento.modules.section.application.port.in;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.EventSectionResponse;

import java.util.List;

public interface EventSectionUseCase {
    List<EventSectionResponse> getSectionsByEvent(Integer eventId);
    EventSectionResponse createEventSection(CreateEventSectionRequest request);
    EventSectionResponse updateEventSection(Integer eventSectionId, UpdateEventSectionRequest request);
    void deleteEventSection(Integer eventSectionId);
}
