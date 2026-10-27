package com.capysoft.tuevento.modules.event.application.port.out;

/**
 * Consulta de sedes desde el dominio de eventos. Las sedes pertenecen al servicio de
 * localización: el evento solo guarda el {@code siteId} y pide nombre y capacidad por aquí.
 */
public interface SiteLookupPort {

    /**
     * Devuelve la sede con ese id.
     *
     * @throws com.capysoft.tuevento.shared.domain.exception.NotFoundException si no existe
     * @throws com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException si el
     *         servicio de localización no responde
     */
    SiteSummary getSite(Integer siteId);
}
