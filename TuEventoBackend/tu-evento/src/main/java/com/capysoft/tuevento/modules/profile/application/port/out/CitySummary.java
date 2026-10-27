package com.capysoft.tuevento.modules.profile.application.port.out;

/**
 * Datos de una ciudad que muestra el perfil.
 */
public record CitySummary(Integer cityId, String name, String departmentName) {
}
