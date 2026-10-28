package com.capysoft.notification.application.port.out;

import java.util.Map;

/**
 * Lo que el servicio necesita que haga el monolito: reenviar los avisos en tiempo real por su
 * /ws (web y móvil siguen conectados allí) y traducir las notificaciones nuevas.
 */
public interface NotificationEventsPort {

    /** Envía {@code body} al topic de notificaciones del usuario ({@code /topic/notifications/{userId}}). */
    void push(Integer userId, Map<String, Object> body);

    /** Pide traducir la notificación desde el español a los demás idiomas activos. */
    void requestTranslation(Long notificationId);
}
