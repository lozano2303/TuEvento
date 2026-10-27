package com.capysoft.tuevento.modules.notification.infrastructure.remote;

/**
 * Topics de Kafka entre el monolito y el microservicio de notificaciones
 * (patrón {@code tuevento.<servicio>.<agregado>}). Deben coincidir con los del servicio.
 */
public final class NotificationTopics {

    /** Monolito → servicio: notificaciones a enviar, con los datos del destinatario. */
    public static final String COMMANDS = "tuevento.notificaciones.command";
    /** Servicio → monolito: pushes para reenviar por el /ws del monolito. */
    public static final String PUSH = "tuevento.notificaciones.push";
    /** Servicio → monolito: notificaciones nuevas que hay que traducir. */
    public static final String TRANSLATION = "tuevento.notificaciones.translation";

    private NotificationTopics() {
    }
}
