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
@Table(name = "account_activation")
public class AccountActivationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_activation_id")
    private Integer accountActivationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "activation_code", nullable = false, length = 8)
    private String activationCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "activated", nullable = false, columnDefinition = "boolean default false")
    private Boolean activated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
