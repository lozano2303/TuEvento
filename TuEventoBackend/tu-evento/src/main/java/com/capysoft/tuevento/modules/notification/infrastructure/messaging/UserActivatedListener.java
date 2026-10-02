package com.capysoft.tuevento.modules.notification.infrastructure.messaging;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.usecase.SendNotificationUseCase;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.security.domain.event.UserActivatedEvent;

import lombok.RequiredArgsConstructor;

/**
 * Listens to UserActivatedEvent and sends a welcome notification after account activation.
 */
@Component
@RequiredArgsConstructor
public class UserActivatedListener {

    private static final Logger log = LoggerFactory.getLogger(UserActivatedListener.class);

    private final SendNotificationUseCase sendNotificationUseCase;
    private final ProfileRepository profileRepository;

    @EventListener
    public void onUserActivated(UserActivatedEvent event) {
        log.info("User activated event detected: userId={}, alias={}", event.getUserId(), event.getAlias());

        try {
            // Get user's full name from profile
            Optional<Profile> profileOpt = profileRepository.findByUserId(event.getUserId());
            String fullName = profileOpt.map(Profile::getFullName).orElse(event.getAlias());

            // Send welcome notification via IN_APP and EMAIL
            SendNotificationCommand command = SendNotificationCommand.builder()
                    .userIds(List.of(event.getUserId()))
                    .typeName(NotificationTypeNames.WELCOME)
                    .entityType("USER")
                    .entityId(event.getUserId().longValue())
                    .reason(fullName) // Use reason field to pass the user's name
                    .build();

            sendNotificationUseCase.execute(command);
            log.info("Welcome notification sent to user: userId={}, name={}", event.getUserId(), fullName);

        } catch (Exception e) {
            log.error("Failed to send welcome notification to user: userId={}, error={}", 
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
