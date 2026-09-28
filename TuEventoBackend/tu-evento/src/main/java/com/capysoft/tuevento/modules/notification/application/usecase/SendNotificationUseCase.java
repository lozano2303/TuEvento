package com.capysoft.tuevento.modules.notification.application.usecase;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.port.out.NotificationChannelPort;
import com.capysoft.tuevento.modules.notification.application.service.NotificationMessageFactory;
import com.capysoft.tuevento.modules.notification.domain.model.Channel;
import com.capysoft.tuevento.modules.notification.domain.model.Notification;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationType;
import com.capysoft.tuevento.modules.notification.domain.repository.ChannelRepository;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationRepository;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SendNotificationUseCase {

    private final ChannelRepository channelRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final NotificationRepository notificationRepository;
    private final Map<String, NotificationChannelPort> channelsByName;
    private final boolean emailEnabled;

    public SendNotificationUseCase(
            ChannelRepository channelRepository,
            NotificationTypeRepository notificationTypeRepository,
            NotificationRepository notificationRepository,
            List<NotificationChannelPort> channelPorts,
            @Value("${notification.email.enabled:true}") boolean emailEnabled) {
        this.channelRepository = channelRepository;
        this.notificationTypeRepository = notificationTypeRepository;
        this.notificationRepository = notificationRepository;
        this.channelsByName = channelPorts.stream()
                .collect(Collectors.toMap(NotificationChannelPort::channelName, Function.identity()));
        this.emailEnabled = emailEnabled;
    }

    @Transactional
    public void execute(SendNotificationCommand command) {
        if (command == null || command.getUserIds() == null || command.getUserIds().isEmpty()) {
            log.warn("SendNotification skipped: missing recipients type={}", command == null ? null : command.getTypeName());
            return;
        }

        NotificationType type = notificationTypeRepository.findByName(command.getTypeName())
                .filter(NotificationType::isActive)
                .orElse(null);
        if (type == null) {
            log.warn("SendNotification skipped: unknown or inactive type={}", command.getTypeName());
            return;
        }

        List<Channel> activeChannels = channelRepository.findAllActive();
        for (Channel channel : activeChannels) {
            if (NotificationChannelNames.EMAIL.equals(channel.getName()) && !emailEnabled) {
                log.debug("EMAIL channel skipped because notification.email.enabled=false");
                continue;
            }

            NotificationChannelPort adapter = channelsByName.get(channel.getName());
            if (adapter == null) {
                log.warn("No adapter registered for channel={}", channel.getName());
                continue;
            }

            String idempotencyKey = command.getTypeName() + ":" + command.getEntityId() + ":" + channel.getName();
            if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.info("Notification already sent, skipping duplicate idempotencyKey={}", idempotencyKey);
                continue;
            }

            NotificationMessageFactory.MessageContent content =
                    NotificationMessageFactory.build(command.getTypeName(), channel.getName(), command);

            LocalDateTime sentAt = NotificationChannelNames.IN_APP.equals(channel.getName())
                    ? LocalDateTime.now()
                    : null;

            Notification saved = notificationRepository.save(Notification.builder()
                    .channelId(channel.getChannelId())
                    .notificationTypeId(type.getNotificationTypeId())
                    .entityType(command.getEntityType())
                    .entityId(command.getEntityId())
                    .subject(truncate(content.subject(), 150))
                    .body(content.body())
                    .sentAt(sentAt)
                    .idempotencyKey(idempotencyKey)
                    .build());

            try {
                adapter.deliver(saved, command.getUserIds());
            } catch (Exception e) {
                log.error("Channel adapter failed without aborting origin flow: channel={}, notificationId={}",
                        channel.getName(), saved.getNotificationId(), e);
            }
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
