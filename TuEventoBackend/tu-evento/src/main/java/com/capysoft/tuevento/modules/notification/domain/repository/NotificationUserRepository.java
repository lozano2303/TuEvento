package com.capysoft.tuevento.modules.notification.domain.repository;

import com.capysoft.tuevento.modules.notification.domain.model.NotificationUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationUserRepository {
    NotificationUser save(NotificationUser notificationUser);

    Page<NotificationUser> findInAppByUserId(Integer userId, boolean unreadOnly, Pageable pageable);

    long countUnreadInAppByUserId(Integer userId);

    Optional<NotificationUser> findByIdAndUserId(Long notificationUserId, Integer userId);

    int markAllInAppRead(Integer userId, LocalDateTime readAt);
}
