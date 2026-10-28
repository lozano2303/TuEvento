package com.capysoft.tuevento.modules.notification.infrastructure.remote;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.port.out.NotificationSender;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Modo {@code local}: la notificación se envía dentro del monolito, como siempre.
 *
 * <p>{@code @Primary} solo desambigua para el análisis estático: este bean y
 * {@link KafkaNotificationSender} dependen de valores distintos de {@code notification.mode},
 * así que en ejecución nunca existen los dos a la vez.
 */
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.mode", havingValue = "local", matchIfMissing = true)
public class LocalNotificationSender implements NotificationSender {

    private final SendNotificationUseCase sendNotificationUseCase;

    @Override
    public void send(SendNotificationCommand command) {
        sendNotificationUseCase.execute(command);
    }
}
