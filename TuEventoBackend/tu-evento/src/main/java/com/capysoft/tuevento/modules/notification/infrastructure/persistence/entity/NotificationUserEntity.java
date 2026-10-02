package com.capysoft.tuevento.modules.notification.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.notification.domain.model.DeliveredStatus;
import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationUserEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_user_id")
    private Long notificationUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private NotificationEntity notification;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivered_status", nullable = false, length = 20)
    private DeliveredStatus deliveredStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
