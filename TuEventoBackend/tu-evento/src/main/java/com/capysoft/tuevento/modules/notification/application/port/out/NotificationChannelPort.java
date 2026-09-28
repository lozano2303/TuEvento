package com.capysoft.tuevento.modules.notification.application.port.out;

import com.capysoft.tuevento.modules.notification.domain.model.Notification;

import java.util.List;

/**
 * Adaptador de un canal de entrega. Seleccionado por {@link #channelName()}.
 * Nunca debe lanzar excepciones hacia el flujo de negocio que originó la notificación.
 */
public interface NotificationChannelPort {

    String channelName();

    void deliver(Notification notification, List<Integer> userIds);
}
