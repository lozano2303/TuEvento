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

    /** Returns all events regardless of status or visibility — for admin use. */
    List<Event> findAll();

    /**
     * Returns all events with status=PUBLISHED, is_public=true
     * and finish_date >= today (active events only).
     */
    List<Event> findAllPublished();

    /**
     * Returns published public active events whose site belongs to the given city.
     * Requires a JOIN to the site table.
     */
    List<Event> findByCityId(Long cityId);

    /**
     * Returns published public active events assigned to the given category.
     * Resolved via category_event join.
     */
    List<Event> findByCategoryId(Integer categoryId);

    /** Returns published public active events whose startDate falls within [from, to]. */
    List<Event> findByDateRange(LocalDate from, LocalDate to);

    /**
     * Full-text search on published active events — no date filter.
     * @param q term to search in event_name (empty string = match all)
     */
    List<Event> searchPublishedByText(String q);

    /**
     * Full-text + exact start_date search on published active events.
     * @param q    term to search in event_name (empty string = match all)
     * @param date exact start_date filter — always non-null
     */
    List<Event> searchPublishedByTextAndDate(String q, LocalDate date);

    /**
     * Returns all PUBLISHED events whose finishDate is before the given date.
     * Used by the auto-completion scheduler to mark expired events as COMPLETED.
     */
    List<Event> findAllPublishedExpiredBefore(LocalDate date);
}
