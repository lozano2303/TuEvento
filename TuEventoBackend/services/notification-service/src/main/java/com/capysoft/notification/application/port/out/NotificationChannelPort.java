package com.capysoft.notification.application.port.out;

import com.capysoft.notification.domain.model.Notification;

import java.util.List;

import com.capysoft.notification.application.dto.NotificationRecipient;

/**
 * Adaptador de un canal de entrega. Seleccionado por {@link #channelName()}.
 * Nunca debe lanzar excepciones hacia el flujo de negocio que originó la notificación.
 */
public interface NotificationChannelPort {

    String channelName();

    void deliver(Notification notification, List<NotificationRecipient> recipients);
}
