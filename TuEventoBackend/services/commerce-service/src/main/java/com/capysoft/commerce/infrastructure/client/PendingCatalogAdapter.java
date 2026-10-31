package com.capysoft.commerce.infrastructure.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.capysoft.commerce.payment.application.port.out.OrganizerSeatsPort;
import com.capysoft.commerce.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;

/**
 * Catálogo todavía no expone su API interna ni sus eventos (fase C2). Hasta entonces este servicio
 * no recibe tráfico (el gateway sigue enviando Comercio al monolito) y lo que dependa de Catálogo
 * responde 503: nunca un dato inventado. En C2 lo sustituyen el cliente REST y la réplica event_ref.
 */
@Component
class PendingCatalogAdapter implements CatalogSeatsPort, EventLookupPort, OrganizerSeatsPort {

    private static ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException("CATALOG_UNAVAILABLE", "The catalog service is not available yet");
    }

    @Override
    public HeldSeats holdForOrder(Long orderId, Long userId, Long eventId, List<Integer> seatIds) {
        throw unavailable();
    }

    @Override
    public void sell(Long orderId, Long buyerId, List<Integer> seatIds, String reason) {
        throw unavailable();
    }

    @Override
    public void release(Long orderId, List<Integer> seatIds, Integer changedBy, String reason) {
        throw unavailable();
    }

    @Override
    public Optional<EventRef> find(Long eventId) {
        throw unavailable();
    }

    @Override
    public Map<Long, EventRef> findAll(Collection<Long> eventIds) {
        throw unavailable();
    }

    @Override
    public List<EventRef> findByOwner(Long ownerId) {
        throw unavailable();
    }

    @Override
    public List<EventSeats> summarizeByOrganizer(Long organizerId) {
        throw unavailable();
    }
}
