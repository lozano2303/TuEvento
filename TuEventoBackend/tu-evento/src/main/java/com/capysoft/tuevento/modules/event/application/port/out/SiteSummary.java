package com.capysoft.tuevento.modules.event.application.port.out;

/**
 * Datos de una sede que necesita el dominio de eventos: el nombre para mostrarla y la
 * capacidad para validar los asientos disponibles.
 */
public record SiteSummary(Integer siteId, String name, Integer capacity) {
}
