package com.capysoft.tuevento.modules.security.application.port.out;

import java.util.Optional;

/**
 * Rol vigente de un usuario según la BD (fuente de verdad, por encima del claim del JWT).
 *
 * @deprecated El filtro JWT ya no consulta la BD en cada petición: un cambio de rol revoca los
 *             tokens del usuario en {@link TokenRevocationPort}. Se conserva porque tests de otros
 *             módulos aún lo mockean; borrarlo cuando dejen de hacerlo.
 */
@Deprecated(forRemoval = true)
public interface CurrentRolePort {
    Optional<String> findRoleCode(Integer userId);
}
