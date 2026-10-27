package com.capysoft.tuevento.modules.event.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Datos de usuarios que Catálogo necesita mostrar (autores de comentarios, organizadores).
 * Los aporta Identidad; el módulo event no lee sus tablas.
 */
public interface UserDirectoryPort {

    /** Usuarios que existen, indexados por {@code userId}; los IDs nulos o desconocidos no aparecen. */
    Map<Integer, UserRef> findUsers(Collection<Integer> userIds);

    default Optional<UserRef> findUser(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(findUsers(List.of(userId)).get(userId));
    }

    /**
     * Lo mínimo que Catálogo usa de un usuario.
     *
     * @param fullName     nombre del perfil; {@code null} si no tiene
     * @param avatarFileId foto de perfil en Archivos; {@code null} si no tiene
     */
    record UserRef(Integer userId, String fullName, String email, Integer avatarFileId) {
    }
}
