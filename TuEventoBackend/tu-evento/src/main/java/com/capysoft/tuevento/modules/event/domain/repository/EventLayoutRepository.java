package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventLayout;

import java.util.Optional;

public interface EventLayoutRepository {

    EventLayout save(EventLayout layout);
    Optional<EventLayout> findByEventId(Long eventId);
}
