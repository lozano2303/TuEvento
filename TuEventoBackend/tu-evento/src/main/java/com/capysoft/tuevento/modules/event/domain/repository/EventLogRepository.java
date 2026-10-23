package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventLog;

import java.time.LocalDateTime;
import java.util.List;

public interface EventLogRepository {

    EventLog save(EventLog eventLog);
    List<EventLog> findByEventId(Long eventId);
    boolean existsByEventIdAndActionInAndOccurredAtBetween(Long eventId, List<String> actions, LocalDateTime start, LocalDateTime end);
}