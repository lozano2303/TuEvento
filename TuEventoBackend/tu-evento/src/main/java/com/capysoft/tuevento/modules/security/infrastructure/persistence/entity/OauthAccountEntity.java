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
@Table(
        name = "oauth_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_oauth_account_user_provider", columnNames = {"user_id", "provider"}),
                @UniqueConstraint(name = "uq_oauth_account_provider_user", columnNames = {"provider", "provider_user_id"})
        }
)
public class OauthAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_account_id")
    private Integer oauthAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;
}
