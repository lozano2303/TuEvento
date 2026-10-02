package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity.NotificationUserEntity;

public interface JpaNotificationUserRepository extends JpaRepository<NotificationUserEntity, Long> {
    
    @Query("SELECT nu FROM NotificationUserEntity nu WHERE nu.notification.notificationId = :notificationId AND nu.userId = :userId")
    Optional<NotificationUserEntity> findByNotificationIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") Integer userId);
    
    @Query("SELECT COUNT(nu) FROM NotificationUserEntity nu " +
           "JOIN nu.notification n " +
           "JOIN n.channel c " +
           "WHERE nu.userId = :userId " +
           "AND c.name = 'IN_APP' " +
           "AND nu.readAt IS NULL")
    long countUnreadByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT nu FROM NotificationUserEntity nu " +
           "JOIN FETCH nu.notification n " +
           "JOIN FETCH n.channel c " +
           "JOIN FETCH n.notificationType nt " +
           "WHERE nu.userId = :userId " +
           "AND c.name = 'IN_APP' " +
           "AND (:unreadOnly = false OR nu.readAt IS NULL) " +
           "ORDER BY n.sentAt DESC, nu.notificationUserId DESC")
    Page<NotificationUserEntity> findInAppNotificationsByUserId(
            @Param("userId") Integer userId, 
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable);
    
    @Modifying
    @Query("UPDATE NotificationUserEntity nu " +
           "SET nu.readAt = CURRENT_TIMESTAMP " +
           "WHERE nu.userId = :userId " +
           "AND nu.notificationUserId IN (" +
           "   SELECT nu2.notificationUserId " +
           "   FROM NotificationUserEntity nu2 " +
           "   JOIN nu2.notification n " +
           "   JOIN n.channel c " +
           "   WHERE nu2.userId = :userId " +
           "   AND c.name = 'IN_APP' " +
           "   AND nu2.readAt IS NULL" +
           ")")
    int markAllAsReadByUserId(@Param("userId") Integer userId);
}