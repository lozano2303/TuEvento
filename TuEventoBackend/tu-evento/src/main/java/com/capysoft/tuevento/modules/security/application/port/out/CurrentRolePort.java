package com.capysoft.tuevento.modules.security.application.port.out;

import java.util.Optional;

/** Rol vigente de un usuario según la BD (fuente de verdad, por encima del claim del JWT). */
public interface CurrentRolePort {
    Optional<String> findRoleCode(Integer userId);
}
