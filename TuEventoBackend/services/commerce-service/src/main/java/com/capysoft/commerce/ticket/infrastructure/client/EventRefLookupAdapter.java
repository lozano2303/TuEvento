package com.capysoft.commerce.ticket.infrastructure.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.infrastructure.persistence.entity.EventRefEntity;
import com.capysoft.commerce.ticket.infrastructure.persistence.repository.EventRefJpaRepository;

import lombok.RequiredArgsConstructor;

/** Lee los eventos de la réplica local {@code event_ref}: Comercio no depende de que Catálogo responda. */
@Component
@RequiredArgsConstructor
class EventRefLookupAdapter implements EventLookupPort {

    private final EventRefJpaRepository repository;

    @Override
    public Optional<EventRef> find(Long eventId) {
        return repository.findById(eventId).map(EventRefLookupAdapter::toRef);
    }

    @Override
    public Map<Long, EventRef> findAll(Collection<Long> eventIds) {
        return repository.findAllById(eventIds).stream()
                .map(EventRefLookupAdapter::toRef)
                .collect(Collectors.toMap(EventRef::eventId, Function.identity()));
    }

    @Override
    public List<EventRef> findByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId).stream().map(EventRefLookupAdapter::toRef).toList();
    }

    private static EventRef toRef(EventRefEntity event) {
        return new EventRef(event.getEventId(), event.getOwnerId(), event.getName(), event.getStatus());
    }
}
