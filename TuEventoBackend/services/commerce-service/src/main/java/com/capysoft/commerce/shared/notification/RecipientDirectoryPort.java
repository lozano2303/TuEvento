package com.capysoft.commerce.shared.notification;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Datos de contacto de los destinatarios de una notificación. Los aporta Identidad; el módulo
 * notification no lee sus tablas.
 */
public interface RecipientDirectoryPort {

    /** Destinatarios que existen, indexados por {@code userId}; los IDs nulos o desconocidos no aparecen. */
    Map<Integer, Recipient> findRecipients(Collection<Integer> userIds);

    default Optional<Recipient> findRecipient(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(findRecipients(List.of(userId)).get(userId));
    }

    /**
     * @param fullName            nombre del perfil; {@code null} si no tiene
     * @param email               {@code null} si el usuario no tiene credenciales
     * @param preferredLanguageId {@code null} si no eligió idioma
     */
    record Recipient(Integer userId, String alias, String fullName, String email, Long preferredLanguageId) {
    }
}
