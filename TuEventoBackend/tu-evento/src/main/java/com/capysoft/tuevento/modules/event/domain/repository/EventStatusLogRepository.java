package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;

import java.util.List;

public interface EventStatusLogRepository {

    EventStatusLog save(EventStatusLog log);
    List<EventStatusLog> findByEventId(Long eventId);
}
