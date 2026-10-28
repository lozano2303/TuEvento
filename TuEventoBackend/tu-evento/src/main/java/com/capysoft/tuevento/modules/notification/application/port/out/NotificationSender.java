package com.capysoft.tuevento.modules.notification.application.port.out;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;

/**
 * Destino de las notificaciones que deciden los listeners del monolito.
 *
 * <p>Con {@code notification.mode=local} (por defecto) se envían aquí mismo; con
 * {@code notification.mode=remote} se publican en Kafka y las envía el microservicio
 * de notificaciones. Los listeners no saben cuál de los dos está activo.
 */
public interface NotificationSender {

    void send(SendNotificationCommand command);
}
