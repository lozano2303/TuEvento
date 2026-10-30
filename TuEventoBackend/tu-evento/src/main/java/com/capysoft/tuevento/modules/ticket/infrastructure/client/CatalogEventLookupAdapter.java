package com.capysoft.tuevento.modules.ticket.infrastructure.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort;

import lombok.RequiredArgsConstructor;

/**
 * Único punto de Comercio que lee eventos de Catálogo. Hoy delega en el repositorio del
 * monolito; después leerá la réplica local {@code event_ref}.
 */
@Component
@RequiredArgsConstructor
class CatalogEventLookupAdapter implements EventLookupPort {

    private final EventRepository eventRepository;

    @Override
    public Optional<EventRef> find(Long eventId) {
        return eventRepository.findById(eventId).map(CatalogEventLookupAdapter::toRef);
    }

    /** Una lectura por id: solo se pide para listas paginadas, y con event_ref será una sola consulta. */
    @Override
    public Map<Long, EventRef> findAll(Collection<Long> eventIds) {
        return eventIds.stream().distinct()
                .map(this::find)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(EventRef::eventId, Function.identity()));
    }

    @Override
    public List<EventRef> findByOwner(Long ownerId) {
        return eventRepository.findByUserId(ownerId).stream().map(CatalogEventLookupAdapter::toRef).toList();
    }

    private static EventRef toRef(Event event) {
        return new EventRef(event.getEventId(), event.getUserId(), event.getEventName(),
                event.getStatus() == null ? null : event.getStatus().name());
    }
}
