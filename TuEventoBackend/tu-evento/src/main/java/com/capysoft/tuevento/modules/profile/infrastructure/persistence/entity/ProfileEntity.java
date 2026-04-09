package com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.entity.CityEntity;
import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "profile")
public class ProfileEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private CityEntity city;

    /** Decoupled from storage module — stored as plain FK integer. */
    @Column(name = "stored_file_id")
    private Integer storedFileId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "bio", length = 255)
    private String bio;
}
