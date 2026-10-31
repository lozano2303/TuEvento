package com.capysoft.commerce.shared.notification;


/**
 * Destino de las notificaciones que deciden los listeners de cada dominio del monolito: se
 * publican en Kafka y las envía notification-service.
 */
public interface NotificationSender {

    void send(SendNotificationCommand command);
}
