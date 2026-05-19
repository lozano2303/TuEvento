package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository {

    Event save(Event event);
    Optional<Event> findById(Long eventId);
    List<Event> findByUserId(Long userId);
    List<Event> findBySiteId(Long siteId);
    List<Event> findByStatus(EventStatus status);
    boolean existsByEventNameAndStartDateAndSiteId(String eventName, LocalDate startDate, Long siteId);
    void delete(Long eventId);

    /** Returns all events with status=PUBLISHED and is_public=true. */
    List<Event> findAllPublished();

    /**
     * Returns published public events whose site belongs to the given city.
     * Requires a JOIN to the site table.
     */
    List<Event> findByCityId(Long cityId);

    /**
     * Returns published public events assigned to the given category.
     * Resolved via category_event join.
     */
    List<Event> findByCategoryId(Integer categoryId);

    /** Returns published public events whose startDate falls within [from, to]. */
    List<Event> findByDateRange(LocalDate from, LocalDate to);
}
