package com.capysoft.commerce.shared.notification;

/** Topic de Kafka hacia notification-service. Debe coincidir con el del servicio. */
public final class NotificationTopics {

    /** Comercio → notification-service: notificaciones a enviar, con los datos del destinatario. */
    public static final String COMMANDS = "tuevento.notificaciones.command";

    private NotificationTopics() {
    }
}
