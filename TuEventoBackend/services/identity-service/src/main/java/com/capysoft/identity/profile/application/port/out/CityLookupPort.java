package com.capysoft.identity.profile.application.port.out;

import java.util.Optional;

import com.capysoft.common.domain.exception.BusinessException;

/**
 * Consulta de ciudades desde el dominio de perfiles. Las ciudades pertenecen al servicio de
 * localización: el perfil solo guarda el {@code cityId}.
 */
public interface CityLookupPort {

    /**
     * Devuelve la ciudad con ese id.
     *
     * @throws com.capysoft.common.domain.exception.NotFoundException si no existe
     * @throws com.capysoft.identity.shared.domain.exception.ServiceUnavailableException si el
     *         servicio de localización no responde
     */
    CitySummary getCity(Integer cityId);

    /**
     * Versión tolerante para mostrar un perfil: sin ciudad, con una ciudad que ya no existe o
     * con el servicio caído devuelve vacío, y el perfil se muestra sin nombre de ciudad.
     */
    default Optional<CitySummary> findCity(Integer cityId) {
        if (cityId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(getCity(cityId));
        } catch (BusinessException e) {
            return Optional.empty();
        }
    }
}
