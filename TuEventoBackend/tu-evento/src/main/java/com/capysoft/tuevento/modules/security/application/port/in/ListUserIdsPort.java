package com.capysoft.tuevento.modules.security.application.port.in;

import java.util.List;

/**
 * Recorre los IDs de todos los usuarios de la plataforma, para procesos masivos como el
 * crédito a todas las carteras.
 */
public interface ListUserIdsPort {

    long countUsers();

    /**
     * Hasta {@code limit} IDs mayores que {@code afterUserId}, en orden ascendente. Para recorrerlos
     * todos se empieza con {@code 0} y se sigue con el último ID recibido hasta que llegue vacío:
     * a diferencia de paginar por número de página, no salta ni repite usuarios aunque se
     * registren otros durante el recorrido.
     */
    List<Integer> findUserIdsAfter(int afterUserId, int limit);
}
