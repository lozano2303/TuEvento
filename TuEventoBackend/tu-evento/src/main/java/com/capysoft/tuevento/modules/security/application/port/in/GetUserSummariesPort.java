package com.capysoft.tuevento.modules.security.application.port.in;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.capysoft.tuevento.modules.security.application.dto.UserSummary;

/**
 * Consulta en lote de los datos públicos de usuarios. Los demás servicios la usan a través de
 * su propio puerto de salida en {@code infrastructure/client/}, nunca leyendo las tablas de
 * Identidad.
 */
public interface GetUserSummariesPort {

    /**
     * Resúmenes de los usuarios que existen, indexados por {@code userId}. Los IDs nulos,
     * repetidos o desconocidos se ignoran. Hace una consulta por tabla, sin importar cuántos IDs lleguen.
     */
    Map<Integer, UserSummary> getSummaries(Collection<Integer> userIds);

    /** Resumen de un usuario, o vacío si no existe. */
    default Optional<UserSummary> getSummary(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getSummaries(List.of(userId)).get(userId));
    }
}
