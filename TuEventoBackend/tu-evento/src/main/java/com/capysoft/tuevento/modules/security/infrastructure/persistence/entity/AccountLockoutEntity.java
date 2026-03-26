package com.capysoft.tuevento.modules.security.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account_lockout")
public class AccountLockoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_lockout_id")
    private Integer accountLockoutId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "failed_attempts", nullable = false, columnDefinition = "int default 0")
    private Integer failedAttempts;

    @Column(name = "window_started_at")
    private LocalDateTime windowStartedAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
}
