package com.capysoft.notification.application.dto;

/**
 * Destinatario de una notificación. El email y el idioma preferido llegan en el mensaje del
 * monolito, así el servicio no consulta las tablas de Identidad.
 *
 * @param email               null si el usuario no tiene email: no se le envía correo
 * @param preferredLanguageId null si no eligió idioma: se usa el español original
 */
public record NotificationRecipient(Integer userId, String email, Long preferredLanguageId) {

    public static NotificationRecipient withoutContact(Integer userId) {
        return new NotificationRecipient(userId, null, null);
    }
}
