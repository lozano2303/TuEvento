package com.capysoft.notification.application.usecase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.notification.application.port.out.NotificationTranslationReader;
import com.capysoft.notification.application.dto.InAppNotificationDetails;
import com.capysoft.notification.application.dto.InAppNotificationPageResponse;
import com.capysoft.notification.application.dto.InAppNotificationResponse;
import com.capysoft.notification.application.dto.NotificationTranslationDTO;
import com.capysoft.notification.domain.model.NotificationUser;
import com.capysoft.notification.domain.repository.NotificationUserRepository;
import com.capysoft.notification.infrastructure.persistence.repository.NotificationUserRepositoryAdapter;
import com.capysoft.common.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InAppNotificationQueryUseCase {

    private final NotificationUserRepository notificationUserRepository;
    private final NotificationUserRepositoryAdapter notificationUserRepositoryAdapter;
    private final NotificationTranslationReader translationReader;

    @Transactional(readOnly = true)
    public InAppNotificationPageResponse listMine(Integer userId, int page, int size, boolean unreadOnly) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        
        Page<InAppNotificationDetails> notificationDetails = notificationUserRepositoryAdapter
                .findInAppDetailsWithNotificationByUserId(userId, unreadOnly, pageable);
        
        Page<InAppNotificationResponse> result = notificationDetails.map(details -> {
            // Cargar traducciones para esta notificación
            List<NotificationTranslationDTO> translations = loadTranslations(details.getNotificationId());
            
            return InAppNotificationResponse.builder()
                    .notificationUserId(details.getNotificationUserId())
                    .type(details.getTypeName())
                    .subject(details.getSubject())
                    .body(details.getBody())
                    .entityType(details.getEntityType())
                    .entityId(details.getEntityId())
                    .sentAt(details.getSentAt())
                    .readAt(details.getReadAt())
                    .read(details.getReadAt() != null)
                    .translations(translations)
                    .build();
        });
                
        return InAppNotificationPageResponse.builder()
                .content(result.getContent())
                .totalElements(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Integer userId) {
        return notificationUserRepository.countUnreadInAppByUserId(userId);
    }

    @Transactional
    public void markRead(Integer userId, Long notificationUserId) {
        NotificationUser row = notificationUserRepository.findByIdAndUserId(notificationUserId, userId)
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found"));
        if (row.getReadAt() != null) {
            return;
        }
        notificationUserRepository.save(NotificationUser.builder()
                .notificationUserId(row.getNotificationUserId())
                .notificationId(row.getNotificationId())
                .userId(row.getUserId())
                .emailAddress(row.getEmailAddress())
                .deliveredStatus(row.getDeliveredStatus())
                .errorMessage(row.getErrorMessage())
                .readAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void markAllRead(Integer userId) {
        notificationUserRepository.markAllInAppRead(userId, LocalDateTime.now());
    }

    /** Carga las traducciones de una notificación con una sola consulta (traducción + código de idioma). */
    private List<NotificationTranslationDTO> loadTranslations(Long notificationId) {
        return translationReader.findAll(notificationId).stream()
                .filter(translation -> translation.languageCode() != null)
                .map(translation -> NotificationTranslationDTO.builder()
                        .languageCode(translation.languageCode())
                        .translatedSubject(translation.subject())
                        .translatedBody(translation.body())
                        .status(translation.status())
                        .source(translation.source())
                        .build())
                .collect(Collectors.toList());
    }
}
