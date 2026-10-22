package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.response.EventResponse;
import com.capysoft.tuevento.modules.event.application.dto.response.EventSummaryResponse;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;

import java.time.LocalDate;
import java.util.List;

public interface GetEventUseCase {

    EventResponse findById(Long eventId);
    EventResponse findById(Long eventId, String languageCode);
    List<EventSummaryResponse> findByUser(Long userId);
    List<EventSummaryResponse> findByStatus(EventStatus status);

    List<EventSummaryResponse> getPublishedEvents();
    List<EventResponse> getPublishedEventsWithTranslations(String languageCode);
    List<EventSummaryResponse> getPublishedEventsByCityId(Long cityId);
    List<EventSummaryResponse> getPublishedEventsByCategoryId(Integer categoryId);
    List<EventSummaryResponse> getPublishedEventsByDateRange(LocalDate from, LocalDate to);

    /**
     * Full-text + optional exact-date search on published public events.
     * @param q    search term (blank = all events)
     * @param date exact start_date filter (null = no date filter)
     */
    List<EventSummaryResponse> searchPublishedEvents(String q, LocalDate date);
}
