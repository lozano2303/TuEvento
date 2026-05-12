package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventMediaLog;

import java.util.List;

public interface EventMediaLogRepository {

    EventMediaLog save(EventMediaLog log);
    List<EventMediaLog> findByEventId(Long eventId);
    int findNextVersionByEventId(Long eventId);
}
