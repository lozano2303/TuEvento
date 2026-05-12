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
}
