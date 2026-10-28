package com.capysoft.tuevento.modules.wallet.application.port.out;

import java.util.List;

/**
 * Usuarios de la plataforma para los créditos masivos. Los aporta Identidad; el módulo wallet
 * no lee sus tablas.
 */
public interface PlatformUsersPort {

    long countUsers();

    /**
     * Hasta {@code limit} IDs mayores que {@code afterUserId}, en orden ascendente. Se empieza con
     * {@code 0} y se sigue con el último recibido hasta que llegue una lista vacía.
     */
    List<Integer> findUserIdsAfter(int afterUserId, int limit);
}
