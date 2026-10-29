package com.capysoft.tuevento.modules.ticket.infrastructure.client;

import java.util.Optional;

import org.springframework.stereotype.Component;

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
        return eventRepository.findById(eventId)
                .map(e -> new EventRef(e.getEventId(), e.getUserId(), e.getEventName(),
                        e.getStatus() == null ? null : e.getStatus().name()));
    }
}
