package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.domain.repository.OrganizerRoleRevocationRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerRoleRevocationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class OrganizerRoleRevocationRepositoryImpl implements OrganizerRoleRevocationRepository {

    private final OrganizerRoleRevocationJpaRepository jpaRepository;

    @Override
    public void record(Integer userId, Integer revokedBy, String reason) {
        jpaRepository.save(OrganizerRoleRevocationEntity.builder()
                .userId(userId)
                .revokedBy(revokedBy)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
