package com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProfileLogJpaRepository extends JpaRepository<ProfileLogEntity, Long> {

    List<ProfileLogEntity> findByProfileId(Long profileId);
    boolean existsByProfileIdAndActionAndOccurredAtBetween(Long profileId, String action, LocalDateTime start, LocalDateTime end);
}
