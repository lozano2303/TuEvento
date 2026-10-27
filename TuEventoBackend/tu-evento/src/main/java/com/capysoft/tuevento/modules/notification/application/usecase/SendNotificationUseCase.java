package com.capysoft.tuevento.modules.notification.application.usecase;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final TranslationRequests translationRequests;
    private final boolean emailEnabled;

    public SendNotificationUseCase(
            ChannelRepository channelRepository,
            NotificationTypeRepository notificationTypeRepository,
            NotificationRepository notificationRepository,
            List<NotificationChannelPort> channelPorts,
            TranslationRequests translationRequests,
            @Value("${notification.email.enabled:true}") boolean emailEnabled) {
        this.channelRepository = channelRepository;
        this.notificationTypeRepository = notificationTypeRepository;
        this.notificationRepository = notificationRepository;
        this.channelsByName = channelPorts.stream()
                .collect(Collectors.toMap(NotificationChannelPort::channelName, Function.identity()));
        this.translationRequests = translationRequests;
        this.emailEnabled = emailEnabled;
    }

    @Transactional
    public void execute(SendNotificationCommand command) {
        log.debug("SendNotificationUseCase.execute() CALLED - Type: {}, Recipients: {}, emailEnabled: {}", 
                command == null ? "NULL" : command.getTypeName(),
                command == null ? "NULL" : command.getUserIds(),
                emailEnabled);
        
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
        log.debug("Active channels found: {}", activeChannels.size());
        for (Channel channel : activeChannels) {
            log.debug("Processing channel: {}", channel.getName());
            
            if (NotificationChannelNames.EMAIL.equals(channel.getName()) && !emailEnabled) {
                log.debug("EMAIL channel SKIPPED because emailEnabled=false");
                continue;
            }

            NotificationChannelPort adapter = channelsByName.get(channel.getName());
            log.debug("Adapter for {}: {}", channel.getName(), adapter == null ? "NULL" : adapter.getClass().getSimpleName());
            if (adapter == null) {
                log.warn("NO ADAPTER for channel={}", channel.getName());
                continue;
            }

            String idempotencyKey = command.getIdempotencySuffix() != null
                    ? command.getTypeName() + ":" + command.getEntityId() + ":" + channel.getName()
                      + ":" + command.getIdempotencySuffix()
                    : command.getTypeName() + ":" + command.getEntityId() + ":" + channel.getName();
            log.debug("Idempotency key: {}", idempotencyKey);
            if (notificationRepository.existsByIdempotencyKey(idempotencyKey)) {
                log.debug("DUPLICATE DETECTED, skipping idempotencyKey={}", idempotencyKey);
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

            // CRITICAL: Flush to ensure notification is committed to DB
            notificationRepository.flush();

            // IN_APP debe ejecutarse INMEDIATAMENTE (antes del commit) para WebSocket
            // EMAIL debe ejecutarse DESPUÉS del commit para evitar "Notification not found"
            final boolean isInApp = NotificationChannelNames.IN_APP.equals(channel.getName());
            
            if (isInApp) {
                // IN_APP: Ejecutar inmediatamente (WebSocket necesita respuesta rápida)
                try {
                    log.info("=== CALLING ADAPTER DELIVER (IN_APP) === channel={}, notificationId={}, recipientCount={}, adapterClass={}", 
                            channel.getName(), saved.getNotificationId(), command.getUserIds().size(), 
                            adapter.getClass().getSimpleName());
                    adapter.deliver(saved, command.getUserIds());
                    log.info("=== ADAPTER DELIVER RETURNED (IN_APP) === channel={}, notificationId={}", 
                            channel.getName(), saved.getNotificationId());
                } catch (Exception e) {
                    log.error("Channel adapter failed without aborting origin flow: channel={}, notificationId={}",
                            channel.getName(), saved.getNotificationId(), e);
                }
            }
            
            // Schedule translation request AND EMAIL delivery AFTER transaction commit
            // This ensures the notification is visible to async operations
            final Long notificationId = saved.getNotificationId();
            final NotificationChannelPort finalAdapter = adapter;
            final String channelName = channel.getName();
            
            TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // Request translation
                        try {
                            translationRequests.request("notification", notificationId);
                            log.debug("Translation requested for notification {}", notificationId);
                        } catch (Exception e) {
                            log.warn("Failed to request translation for notification {}: {}", notificationId, e.getMessage());
                        }
                        
                        // Deliver EMAIL notification AFTER commit (avoids "Notification not found")
                        // IN_APP already executed before commit
                        if (!isInApp) {
                            try {
                                log.info("=== CALLING ADAPTER DELIVER (EMAIL) === channel={}, notificationId={}, recipientCount={}, adapterClass={}", 
                                        channelName, notificationId, command.getUserIds().size(), 
                                        finalAdapter.getClass().getSimpleName());
                                finalAdapter.deliver(saved, command.getUserIds());
                                log.info("=== ADAPTER DELIVER RETURNED (EMAIL) === channel={}, notificationId={}", 
                                        channelName, notificationId);
                            } catch (Exception e) {
                                log.error("Channel adapter failed without aborting origin flow: channel={}, notificationId={}",
                                        channelName, notificationId, e);
                            }
                        }
                    }
                }
            );
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
