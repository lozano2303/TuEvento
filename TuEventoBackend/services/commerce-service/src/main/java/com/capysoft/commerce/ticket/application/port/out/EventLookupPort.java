package com.capysoft.commerce.ticket.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Datos de un evento que necesita Comercio (ticket, payment y wallet): dueño, nombre y estado.
 * Los aporta Catálogo.
 */
public interface EventLookupPort {

    /** El evento pedido, o vacío si no existe. */
    Optional<EventRef> find(Long eventId);

    /** Los eventos pedidos que existan, por id. */
    Map<Long, EventRef> findAll(Collection<Long> eventIds);

    /** Los eventos de un organizador. */
    List<EventRef> findByOwner(Long ownerId);

    /**
     * @param ownerId userId del organizador dueño del evento
     * @param status  nombre del estado del evento en Catálogo (p. ej. {@code PUBLISHED})
     */
    record EventRef(Long eventId, Long ownerId, String name, String status) {

        public static final String PUBLISHED = "PUBLISHED";

        /** Solo un evento publicado admite validar entradas. */
        public boolean isPublished() {
            return PUBLISHED.equals(status);
        }
    }
}
